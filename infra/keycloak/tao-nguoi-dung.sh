#!/usr/bin/env bash
# Admin tạo một tài khoản trong realm finquery (tên đăng nhập = email công ty). Bản bash của tao-nguoi-dung.ps1,
# chạy được trên Linux, macOS, Git Bash (Windows).
#   ./tao-nguoi-dung.sh --email nguyen.van.a@viettel.com.vn --last-name 'Nguyễn' --first-name 'Văn A'          # analyst: hỏi đáp
#   ./tao-nguoi-dung.sh --email tai.lieu@viettel.com.vn --last-name 'Tài' --first-name 'Liệu' --role document_manager
#   ./tao-nguoi-dung.sh --email quan.tri@viettel.com.vn --last-name 'Quản' --first-name 'Trị' --role admin
#   thêm --permanent cho tài khoản demo (giám khảo): mật khẩu cố định, không bắt đổi ở lần đăng nhập đầu
#
# - Mật khẩu nhập lúc chạy (không hiện ký tự), không nằm trong file hay trên dòng lệnh.
# - Mặc định là mật khẩu TẠM: người dùng phải đổi ở lần đăng nhập đầu, admin không biết mật khẩu thật.
#   --permanent chỉ dùng cho tài khoản demo; mật khẩu đưa trực tiếp, không ghi vào repo, slide hay tin nhắn.
# - Keycloak kiểm chính sách mật khẩu và email công ty; vi phạm thì báo lỗi, không tạo tài khoản.
# - KC_CONTAINER: tên container Keycloak (mặc định finq-keycloak).
set -euo pipefail

# Git Bash: không đổi /opt/... thành đường dẫn Windows khi truyền cho docker exec
export MSYS_NO_PATHCONV=1

email='' first='' last='' role='analyst' permanent=false
usage() {
    echo "Cách dùng: $0 --email <x@viettel.com.vn> --first-name <tên> --last-name <họ> [--role analyst|document_manager|admin] [--permanent]" >&2
    exit 2
}
while [ $# -gt 0 ]; do
    case "$1" in
        --email) email="${2:-}"; shift 2 ;;
        --first-name) first="${2:-}"; shift 2 ;;
        --last-name) last="${2:-}"; shift 2 ;;
        --role) role="${2:-}"; shift 2 ;;
        --permanent) permanent=true; shift ;;
        -h|--help) usage ;;
        *) echo "Tham số lạ: $1" >&2; usage ;;
    esac
done
# họ tên bắt buộc (kiểu Việt Nam: họ = LastName đứng trước, tên = FirstName): realm yêu cầu họ tên, thiếu thì Keycloak đòi bổ sung hồ sơ ở lần đăng nhập đầu
[ -n "$email" ] && [ -n "$first" ] && [ -n "$last" ] || usage
case "$role" in analyst|document_manager|admin) ;; *) echo "Vai trò không hợp lệ: $role" >&2; usage ;; esac

container="${KC_CONTAINER:-finq-keycloak}"
kcadm=/opt/keycloak/bin/kcadm.sh

# chuỗi JSON: thoát \ và " (họ tên, mật khẩu); mật khẩu không có dấu nháy theo chính sách nhưng vẫn thoát cho chắc
json_str() {
    local s=${1//\\/\\\\}
    s=${s//\"/\\\"}
    printf '"%s"' "$s"
}

cleanup() { unset KC_CLI_PASSWORD password user_json; }
trap cleanup EXIT

# KC_ADMIN_PASSWORD trong .env theo quy tắc của Docker Compose: '...' giữ nguyên, "..." bỏ nháy, không nháy thì $$ -> $
env_password() {
    local f="$1" line v
    [ -f "$f" ] || return 1
    line=$(grep -E '^[[:space:]]*KC_ADMIN_PASSWORD[[:space:]]*=' "$f" | head -n 1 | tr -d '\r') || return 1
    [ -n "$line" ] || return 1
    v=${line#*=}
    v="${v#"${v%%[![:space:]]*}"}"; v="${v%"${v##*[![:space:]]}"}"
    if [[ $v == \'*\' ]]; then v=${v:1:${#v}-2}
    elif [[ $v == \"*\" ]]; then v=${v:1:${#v}-2}
    else v=${v//\$\$/\$}; fi
    printf '%s' "$v"
}

# mật khẩu admin đi qua biến môi trường (docker exec -e KC_CLI_PASSWORD không kèm giá trị), không lên dòng lệnh;
# lấy từ .env cạnh script (khỏi gõ sai vì bộ gõ tiếng Việt), không có thì hỏi
if KC_CLI_PASSWORD=$(env_password "$(dirname "$0")/.env") && [ -n "$KC_CLI_PASSWORD" ]; then
    echo 'Dùng KC_ADMIN_PASSWORD trong .env' >&2
else
    read -r -s -p 'Mật khẩu admin của Keycloak (KC_ADMIN_PASSWORD): ' KC_CLI_PASSWORD; echo >&2
fi
export KC_CLI_PASSWORD
docker exec -e KC_CLI_PASSWORD "$container" "$kcadm" config credentials --server http://localhost:8180 --realm master --user admin \
    || { echo 'Đăng nhập admin Keycloak thất bại (nếu gõ tay: tắt bộ gõ tiếng Việt, không gõ dấu nháy hay $$ của file .env)' >&2; exit 1; }
unset KC_CLI_PASSWORD

if $permanent; then kind='cố định'; temporary=false; else kind='tạm'; temporary=true; fi
read -r -s -p "Mật khẩu $kind cho $email: " password; echo >&2

# mật khẩu người dùng đi qua stdin dưới dạng JSON; admin tạo từ danh bạ công ty nên coi email đã xác minh
user_json="{\"username\":$(json_str "$email"),\"email\":$(json_str "$email"),\"firstName\":$(json_str "$first"),\"lastName\":$(json_str "$last"),\"enabled\":true,\"emailVerified\":true,\"credentials\":[{\"type\":\"password\",\"value\":$(json_str "$password"),\"temporary\":$temporary}]}"
printf '%s' "$user_json" | docker exec -i "$container" "$kcadm" create users -r finquery -f - \
    || { echo 'Không tạo được tài khoản (xem lỗi chính sách mật khẩu / email ở trên)' >&2; exit 1; }

# người dùng mới chưa có quyền gì (default role không gồm analyst): gán một vai trò;
# vai trò lồng nhau admin ⊃ document_manager ⊃ analyst nên không cần gán vai trò bên dưới
docker exec "$container" "$kcadm" add-roles -r finquery --uusername "$email" --rolename "$role" \
    || { echo "Không gán được vai trò $role" >&2; exit 1; }

msg="Đã tạo $email (vai trò: $role)"
$permanent || msg="$msg; lần đăng nhập đầu phải đổi mật khẩu"
echo "$msg."
