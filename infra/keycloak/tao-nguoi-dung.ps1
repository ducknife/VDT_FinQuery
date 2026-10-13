# Admin tạo một tài khoản trong realm finquery (tên đăng nhập = email công ty).
#   .\tao-nguoi-dung.ps1 -Email nguyen.van.a@viettel.com.vn -LastName 'Nguyễn' -FirstName 'Văn A'  # analyst: hỏi đáp
#   .\tao-nguoi-dung.ps1 -Email tai.lieu@viettel.com.vn -LastName 'Tài' -FirstName 'Liệu' -Role document_manager  # + nạp, quản lý tài liệu
#   .\tao-nguoi-dung.ps1 -Email quan.tri@viettel.com.vn -LastName 'Quản' -FirstName 'Trị' -Role admin  # + duyệt, quản lý tài khoản
#   thêm -Permanent cho tài khoản demo (giám khảo): mật khẩu cố định, không bắt đổi ở lần đăng nhập đầu
#
# - Mật khẩu nhập lúc chạy (ẩn ký tự), không nằm trong file hay trên dòng lệnh.
# - Mặc định là mật khẩu TẠM: người dùng phải đổi ở lần đăng nhập đầu, admin không biết mật khẩu thật.
#   -Permanent chỉ dùng cho tài khoản demo; mật khẩu đưa trực tiếp, không ghi vào repo, slide hay tin nhắn.
# - Keycloak kiểm chính sách mật khẩu (từ 12 ký tự, hoa, thường, số, ký tự đặc biệt, không dấu nháy,
#   không chứa tên công ty/hệ thống, không chứa email). Vi phạm thì báo lỗi, không tạo tài khoản.
param(
    [Parameter(Mandatory = $true)] [string] $Email,
    [ValidateSet('analyst', 'document_manager', 'admin')] [string] $Role = 'analyst',
    [switch] $Permanent,
    # họ tên bắt buộc (kiểu Việt Nam: họ = LastName đứng trước, tên = FirstName): realm yêu cầu họ tên, thiếu thì Keycloak đòi bổ sung hồ sơ ở lần đăng nhập đầu
    [Parameter(Mandatory = $true)] [string] $FirstName,
    [Parameter(Mandatory = $true)] [string] $LastName
)

$ErrorActionPreference = 'Stop'

function Read-Plain([string] $prompt) {
    $secure = Read-Host -AsSecureString $prompt
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
}

# KC_ADMIN_PASSWORD trong .env theo quy tắc của Docker Compose: '...' giữ nguyên, "..." bỏ nháy, không nháy thì $$ -> $
function Read-EnvPassword([string] $path) {
    if (-not (Test-Path $path)) { return $null }
    $line = Get-Content -Encoding UTF8 $path | Where-Object { $_ -match '^\s*KC_ADMIN_PASSWORD\s*=' } | Select-Object -First 1
    if (-not $line) { return $null }
    $v = ($line -split '=', 2)[1].Trim()
    if ($v -match "^'(.*)'$") { return $Matches[1] }
    if ($v -match '^"(.*)"$') { return $Matches[1] }
    return $v.Replace('$$', '$')
}

# tạo tài khoản từ JSON. Không đẩy JSON qua stdin: PowerShell 5.1 (pipe, kể cả Process.StandardInput) tự chèn
# BOM (EF BB BF) vào đầu, Keycloak trả "400 Bad Request". JSON đi dưới dạng base64 UTF-8 không BOM qua biến
# môi trường (docker exec -e KC_USER_JSON không kèm giá trị: không lên dòng lệnh), giải mã trong container
function New-KeycloakUser([string] $json) {
    $env:KC_USER_JSON = [Convert]::ToBase64String((New-Object System.Text.UTF8Encoding $false).GetBytes($json))
    try {
        & docker exec -e KC_USER_JSON finq-keycloak sh -c 'printf %s $KC_USER_JSON | base64 -d | /opt/keycloak/bin/kcadm.sh create users -r finquery -f -' | Out-Host
        return $LASTEXITCODE
    }
    finally { Remove-Item Env:KC_USER_JSON -ErrorAction SilentlyContinue }
}

$kcadm = '/opt/keycloak/bin/kcadm.sh'
try {
    # mật khẩu admin đi qua biến môi trường (docker exec -e KC_CLI_PASSWORD không kèm giá trị), không lên dòng lệnh;
    # lấy từ .env cạnh script (khỏi gõ sai vì bộ gõ tiếng Việt), không có thì hỏi
    $adminPass = Read-EnvPassword (Join-Path $PSScriptRoot '.env')
    if (-not $adminPass) { $adminPass = Read-Plain 'Mật khẩu admin của Keycloak (KC_ADMIN_PASSWORD)' }
    else { Write-Host 'Dùng KC_ADMIN_PASSWORD trong .env' }
    $env:KC_CLI_PASSWORD = $adminPass
    $adminPass = $null
    & docker exec -e KC_CLI_PASSWORD finq-keycloak $kcadm config credentials --server http://localhost:8180 --realm master --user admin
    if ($LASTEXITCODE -ne 0) { throw 'Đăng nhập admin Keycloak thất bại (nếu gõ tay: tắt bộ gõ tiếng Việt, không gõ dấu nháy hay $$ của file .env)' }

    $password = Read-Plain "Mật khẩu $(if ($Permanent) { 'cố định' } else { 'tạm' }) cho $Email"
    # mật khẩu người dùng nằm trong JSON (base64 qua biến môi trường, xem New-KeycloakUser)
    $user = @{
        username      = $Email
        email         = $Email
        firstName     = $FirstName
        lastName      = $LastName
        enabled       = $true
        # admin tạo từ danh bạ công ty nên coi email đã xác minh; người tự đăng ký thì phải bấm link xác nhận
        emailVerified = $true
        credentials   = @(@{ type = 'password'; value = $password; temporary = (-not $Permanent) })
    } | ConvertTo-Json -Depth 4 -Compress
    $code = New-KeycloakUser $user
    if ($code -ne 0) { throw 'Không tạo được tài khoản (xem lỗi chính sách mật khẩu / email ở trên)' }

    # người dùng mới chưa có quyền gì (default role không gồm analyst): gán một vai trò;
    # vai trò lồng nhau admin ⊃ document_manager ⊃ analyst nên không cần gán vai trò bên dưới
    & docker exec finq-keycloak $kcadm add-roles -r finquery --uusername $Email --rolename $Role
    if ($LASTEXITCODE -ne 0) { throw "Không gán được vai trò $Role" }
    Write-Host "Đã tạo $Email (vai trò: $Role)$(if (-not $Permanent) { '; lần đăng nhập đầu phải đổi mật khẩu' })."
}
finally {
    Remove-Item Env:KC_CLI_PASSWORD -ErrorAction SilentlyContinue
    $password = $null
    $user = $null
}
