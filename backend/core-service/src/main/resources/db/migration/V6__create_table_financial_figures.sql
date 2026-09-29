-- ---------------------------------------------------------------------------
-- financial_figures — SỐ LIỆU. Đây là bảng nhánh SQL thực sự truy vấn.
--
-- Ba bảng trước chỉ phục vụ bảng này:
--   financial_items          con số này NGHĨA LÀ GÌ
--   financial_item_mappings  dịch mã in trên giấy sang mã nội bộ (dùng lúc nạp)
--   documents                con số này TỪ ĐÂU RA
--
-- BẢNG DỌC, không phải bảng ngang: mỗi con số một DÒNG, không phải một Ô.
-- Lý do (xếp theo sức nặng):
--   1. dẫn chứng theo từng con số — bảng ngang phải thêm 200 cột để chứa số trang
--   2. thêm chỉ tiêu không phải ALTER TABLE
--   3. báo cáo quý thiếu chỉ tiêu → bảng ngang đầy NULL
--   4. chuỗi SQL là hằng số lúc biên dịch, mô hình chỉ điền GIÁ TRỊ vào dấu ?
--
-- HAI CỘT NGÀY, không phải một. Vì có hai loại số khác bản chất:
--   thời điểm  Tổng tài sản tại 30/09/2019     tu_ngay = den_ngay
--   thời kỳ    Doanh thu quý 3/2019            tu_ngay 01/07, den_ngay 30/09
--              Doanh thu luỹ kế 9 tháng 2019   tu_ngay 01/01, den_ngay 30/09
-- Đo trên kho: 674 trang / 110 tài liệu có cột "luỹ kế" song song cột "trong kỳ".
-- Dùng một cột `nam` hay một cột `ngay` là gộp nhầm hai con số khác nhau.
-- ---------------------------------------------------------------------------

CREATE TABLE financial_figures (
    id              BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT,

    -- --- con số này là gì ---------------------------------------------------
    ma_noi_bo       VARCHAR(48)       NOT NULL,
    tu_ngay         DATE              NOT NULL  COMMENT 'Thời điểm: bằng den_ngay. Thời kỳ: mốc đầu kỳ',
    den_ngay        DATE              NOT NULL  COMMENT 'Mốc chốt số liệu',
    pham_vi         VARCHAR(12)       NOT NULL  COMMENT 'hop_nhat | rieng',
    gia_tri         BIGINT            NOT NULL  COMMENT 'Đơn vị ĐỒNG, không thập phân. Âm cho dự phòng, hao mòn, dòng chi',

    -- --- con số này từ đâu ra -----------------------------------------------
    -- Ghi lúc NẠP, không đi tìm lúc trả lời: script đọc tệp nào thì đã biết
    -- tài liệu nào, trang mấy. Chi phí tìm dẫn chứng bằng 0.
    tai_lieu_id     VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    trang           SMALLINT UNSIGNED NOT NULL  COMMENT 'Số trang chứa con số — KHÁC documents.tong_so_trang',
    cach_lay        VARCHAR(16)       NOT NULL DEFAULT 'cot_ky_nay'
                    COMMENT 'cot_ky_nay | cot_ky_truoc — cùng một số có thể lấy từ cột kỳ này của báo cáo A hoặc cột so sánh của báo cáo B',

    -- --- ca trình bày lại ---------------------------------------------------
    -- Có thật: CTR phải trình bày lại tổng tài sản hợp nhất 31/12/2022 theo
    -- thông báo Kiểm toán Nhà nước, lệch 19.047.848.409 đồng.
    --   số đã công bố    6.040.964.527.680   nằm ở 7 tài liệu
    --   số trình bày lại 6.060.012.376.089   nằm ở 4 tài liệu
    -- RAG thuần không quyết được cái nào đúng. Ở đây thì quyết được, và giữ
    -- luôn dấu vết số cũ để trả lời cho đầy đủ.
    trang_thai      VARCHAR(20)       NOT NULL DEFAULT 'hieu_luc'
                    COMMENT 'hieu_luc | da_thay_the',
    -- KHÔNG đặt khoá ngoại tự trỏ cho cột này. Nó chỉ dùng cho ca trình bày lại
    -- (hiếm: 1 ca trong 8 năm), và mã ETL là nơi duy nhất ghi nó — kiểm ở đó rẻ
    -- hơn và rõ hơn. Đổi lại, lược đồ không mang thêm một móc ngược.
    -- ETL phải tự bảo đảm: thay_the_id trỏ tới dòng CÓ THẬT, và khác chính nó.
    thay_the_id     BIGINT UNSIGNED       NULL
                    COMMENT 'Dòng MỚI trỏ về id của dòng CŨ mà nó thay thế — không ràng buộc ở DB',
    ly_do_thay_the  VARCHAR(200)          NULL,

    ngay_nap        DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Khoá duy nhất qua cột sinh: mỗi kỳ chỉ MỘT dòng hiệu lực, nhưng dòng cũ
    -- đã bị thay vẫn nằm thoải mái (NULL không đụng nhau trong UNIQUE).
    --
    -- GIẢ ĐỊNH ĐANG ÁP DỤNG: một kỳ = MỘT dòng. Tổng tài sản 31/12/2024 có mặt
    -- ở 10 tài liệu; ta chọn nguồn có thẩm quyền nhất chứ không lưu 10 dòng:
    --     1. báo cáo năm đã kiểm toán, cột kỳ này
    --     2. báo cáo giữa niên độ đã soát xét, cột kỳ này
    --     3. cột so sánh của báo cáo kỳ sau
    --     + cùng bậc thì ưu tiên documents.la_ban_chinh = 1
    -- Đó chính là điều làm DB hơn RAG: RAG phải chọn 1 trong 10, DB không có gì
    -- để chọn sai. Muốn đổi sang 10 dòng thì phải đổi khoá này.
    khoa_hieu_luc   VARCHAR(140) GENERATED ALWAYS AS (
                        IF(trang_thai = 'hieu_luc',
                           CONCAT_WS('|', ma_noi_bo, tu_ngay, den_ngay, pham_vi),
                           NULL)) STORED,

    PRIMARY KEY (id),
    UNIQUE KEY uk_figures_hieu_luc (khoa_hieu_luc),

    CONSTRAINT fk_figures_ma_noi_bo
        FOREIGN KEY (ma_noi_bo) REFERENCES financial_items (ma_noi_bo),
    CONSTRAINT fk_figures_tai_lieu
        FOREIGN KEY (tai_lieu_id) REFERENCES documents (tai_lieu_id),

    CONSTRAINT ck_figures_ngay      CHECK (tu_ngay <= den_ngay),
    CONSTRAINT ck_figures_pham_vi   CHECK (pham_vi IN ('hop_nhat', 'rieng')),
    CONSTRAINT ck_figures_cach_lay  CHECK (cach_lay IN ('cot_ky_nay', 'cot_ky_truoc')),
    CONSTRAINT ck_figures_trangthai CHECK (trang_thai IN ('hieu_luc', 'da_thay_the'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'So lieu chi tieu tai chinh — bang doc, moi con so mot dong';

-- KHÔNG tạo chỉ số nào ngoài khoá và khoá ngoại.
-- Khoá ngoại fk_figures_ma_noi_bo đã tự sinh chỉ số trên (ma_noi_bo) — lọc theo
-- mã là còn ~60 dòng trong ~7.000, quét nốt mất vài micro giây. Thêm chỉ số ghép
-- theo phỏng đoán chỉ tốn chi phí ghi. Đo thấy chậm thì thêm bằng migration mới.
--
-- Hai thứ CỐ Ý không đặt ở tầng DB, chặn ở mã Java:
--   · thay_the_id trỏ tới dòng có thật  — không dùng khoá ngoại tự trỏ
--   · thay_the_id <> id                  — MySQL cấm CHECK tham chiếu AUTO_INCREMENT
--   · vòng A→B→A khi đi theo thay_the_id — đặt trần 2–3 bước
-- Cột này chỉ dùng cho ca trình bày lại (1 ca trong 8 năm dữ liệu), và chỉ mã ETL
-- ghi nó. Kiểm tại đó rẻ hơn và đọc rõ hơn là mang thêm một móc ngược trong lược đồ.
