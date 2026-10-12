# Dựng bộ 50 câu hỏi đánh giá (eval_test.yml) từ khai báo bên dưới.
# Mỗi câu khai báo CHUỖI BẰNG CHỨNG (nguyên văn trong trang OCR) và tài liệu chính.
# Script quét toàn bộ trang OCR của kho, tìm MỌI trang chứa chuỗi bằng chứng và chấm mức:
#     2 = trang thuộc tài liệu chính (kể cả bản trùng nội dung cùng ngày)
#     1 = trang khác cũng chứa đúng chuỗi đó (BCTN nhắc lại, báo cáo kỳ sau ghi cột kỳ trước...)
# Tài liệu chính mà không chứa chuỗi bằng chứng -> dừng, báo lỗi (không gán nhầm đáp án).
# Phép tính so sánh / tỷ lệ tính bằng code, không tính tay.
#
# Chạy (cần MySQL đang chạy, đọc kết nối từ backend/core-service/.env, không in mật khẩu):
#     python data/eval/dung_bo_cau_hoi.py
import os, re, glob, sys
from decimal import Decimal, ROUND_HALF_UP

import pymysql
import yaml

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
OCR = os.path.join(ROOT, 'data', 'ocr')
OUT = os.path.join(ROOT, 'data', 'eval', 'eval_test.yml')
MAX_HITS = 40


def env():
    out = {}
    for line in open(os.path.join(ROOT, 'backend', 'core-service', '.env'), encoding='utf-8'):
        s = line.strip()
        if s and not s.startswith('#') and '=' in s:
            k, v = s.split('=', 1)
            out[k.strip()] = v.strip().strip('"')
    return out


def load_pages():
    e = env()
    m = re.match(r'jdbc:mysql://([^:/]+):(\d+)/(\w+)', e['DB_URL'])
    con = pymysql.connect(host=m.group(1), port=int(m.group(2)), database=m.group(3),
                          user=e['DB_USERNAME'], password=e.get('DB_PASSWORD', ''), charset='utf8mb4')
    with con, con.cursor() as cur:
        cur.execute('SELECT tai_lieu_id, ma_cong_ty, duong_dan, ten_hien_thi FROM documents')
        docs = cur.fetchall()
    pages, titles = [], {}
    for tid, ma, path, title in docs:
        titles[tid] = title
        for f in sorted(glob.glob(os.path.join(OCR, ma, path.replace('/', os.sep), 'trang_*.md'))):
            n = int(re.search(r'trang_(\d+)', f).group(1))
            pages.append((tid, n, open(f, encoding='utf-8').read()))
    return pages, titles


def ty(dong):
    """đồng -> chuỗi tỷ đồng, 2 chữ số thập phân, dấu phẩy thập phân"""
    v = (Decimal(dong) / Decimal(10**9)).quantize(Decimal('0.01'), ROUND_HALF_UP)
    s = f'{v:,.2f}'.replace(',', '_').replace('.', ',').replace('_', '.')
    return s + ' tỷ đồng'


def pct(a, b):
    """a/b dạng phần trăm 2 chữ số"""
    v = (Decimal(a) / Decimal(b) * 100).quantize(Decimal('0.01'), ROUND_HALF_UP)
    return str(v).replace('.', ',') + '%'


def dong(v):
    return f'{v:,}'.replace(',', '.') + ' đồng'


# ---------------------------------------------------------------------------------------------
# Khai báo câu hỏi.
#   bang_chung: [(chuỗi nguyên văn, [tiền tố tai_lieu_id của tài liệu chính], [regex giới hạn phạm vi quét])]
#   de_nham:    [(chuỗi, ghi chú)] trang trông giống nhưng SAI (khác năm, khác cột, số cũ) — để phân tích reranker
#   bien_the:   ca biên mà câu hỏi kiểm (viết tắt, không dấu, thời gian tương đối...)
# ---------------------------------------------------------------------------------------------
DT_HN_2024 = 12_609_519_560_081
DT_HN_2025 = 13_939_517_269_142
DT_R_2025 = 11_791_011_025_399
LNG_HN_2024 = 884_221_709_521
NPT_HN_2025 = 6_118_368_595_239
TTS_HN_2025 = 8_185_544_854_669
LNST_HN_6T2026 = 321_807_293_831
LNST_HN_6T2025 = 266_926_353_427
LCTT_HDKD_HN_2025 = 1_455_415_537_450
LNST_HN_2025 = 599_717_125_636
HTK_HN_2025 = 424_971_712_679
HTK_HN_2024 = 610_480_085_129

Q = [
    # ---------------- RAG: đoạn văn (giải thích, lý do, quyết định) ----------------
    dict(id='V01', nhom='rag_van_ban', bien_the=['cau_vi_sao', 'pham_vi_rieng'],
         cau_hoi='Vì sao lợi nhuận sau thuế trên báo cáo tài chính riêng quý 2/2024 tăng so với cùng kỳ?',
         dap_an='LNST riêng quý II/2024 đạt 139,3 tỷ, tăng 20,8 tỷ (17,6%) so với cùng kỳ 2023: doanh thu tăng 2% '
                '(2.217,5 tỷ), chi phí tài chính giảm 30% (14,5 tỷ), chi phí quản lý doanh nghiệp giảm 52% (24,6 tỷ).',
         bang_chung=[('139,3 tỷ tăng 20,8 tỷ', ['CTR-GIAITRINH-20240728'], [])],
         de_nham=[('125,1 tỷ, tăng 0,9 tỷ', 'cùng công văn nhưng là số hợp nhất, không phải riêng')]),
    dict(id='V02', nhom='rag_van_ban', bien_the=['cau_vi_sao', 'cach_goi_doi_thuong'],
         cau_hoi='Nguyên nhân lãi ròng hợp nhất quý 3/2025 tăng so với cùng kỳ là gì?',
         dap_an='LNST hợp nhất quý III/2025 đạt 168,9 tỷ, tăng 22,7 tỷ (15,50%): doanh thu 3.965,2 tỷ (+14,09%), '
                'doanh thu tài chính 24,7 tỷ (+100,42%), trong khi giá vốn tăng 14,22%, chi phí tài chính tăng 51,07%.',
         bang_chung=[('168,9 tỷ, tăng 22,7 tỷ', ['CTR-GIAITRINH-20251025'], [])],
         de_nham=[('156,6 tỷ tăng 17,63 tỷ', 'cùng công văn, số của báo cáo riêng')]),
    dict(id='V03', nhom='rag_van_ban', bien_the=['cau_vi_sao', 'khong_dau'],
         cau_hoi='vi sao loi nhuan sau thue hop nhat nam 2025 cua CTR tang so voi nam 2024',
         dap_an='LNST hợp nhất 2025 đạt 599,72 tỷ, tăng 61,50 tỷ (11,43%): doanh thu 13.939,52 tỷ (+10,55%), '
                'doanh thu tài chính 99,62 tỷ (+103,05%); giá vốn tăng 10,51%, chi phí QLDN tăng 46,87%.',
         bang_chung=[('599,72 tỷ, tăng 61,50 tỷ', ['CTR-GIAITRINH-20260316'], [])]),
    dict(id='V04', nhom='rag_van_ban', bien_the=['ngay_thang'],
         cau_hoi='Cổ tức năm 2024 được chi trả theo tỷ lệ bao nhiêu và ngày đăng ký cuối cùng là ngày nào?',
         dap_an='Cổ tức năm 2024 trả bằng tiền tỷ lệ 21,5% (01 cổ phiếu nhận 2.150 đồng); ngày đăng ký cuối cùng 01/10/2025.',
         bang_chung=[('là ngày 01/10/2025', ['CTR-CBTTCOTUC-20250911'], [])]),
    dict(id='V05', nhom='rag_van_ban', bien_the=[],
         cau_hoi='Công ty kiểm toán nào kiểm toán báo cáo tài chính năm 2024 của Viettel Construction?',
         dap_an='Công ty TNHH Hãng Kiểm toán AASC.',
         # bổ sung 12/10/2026 sau benchmark lần 2 (đã kiểm tay): báo cáo kiểm toán trong BCTC/BCTN năm 2024 cũng
         # trả lời đúng ("AASC đã thực hiện kiểm toán các Báo cáo tài chính"); chữ "Hãng" bị OCR đọc thành "Hằng"
         bang_chung=[(['năm 2024 là Công ty TNHH Hãng Kiểm toán AASC', 'đã thực hiện kiểm toán các Báo cáo tài chính'],
                      ['CTR-NGHIQUYET-20250326', 'CTR-BCTN-20241231', 'CTR-BCTC-HN-20241231-NAM', 'CTR-BCTC-R-20241231-NAM'],
                      [r'CTR-NGHIQUYET-2025', r'CTR-BCTN-20241231', r'CTR-BCTC-(HN|R)-20241231-NAM',
                       r'CTR-BAOCAODHDCD-2025', r'CTR-TOTRINH-2025'])]),
    dict(id='V06', nhom='rag_van_ban', bien_the=['viet_tat'],
         cau_hoi='HĐQT nhiệm kỳ 2025-2030 gồm những thành viên nào?',
         dap_an='Ông Đỗ Mạnh Hùng, ông Phạm Đình Trường, bà Vũ Thị Mai, ông Nguyễn Tát Trường, '
                'ông Nguyễn Quang Khải (thành viên độc lập).',
         bang_chung=[('vào Hội đồng quản trị Tổng Công ty nhiệm kỳ 2025-2030', ['CTR-NGHIQUYET-20250326'], [])]),
    dict(id='V07', nhom='rag_van_ban', bien_the=[],
         cau_hoi='Theo báo cáo năm 2025, Tổng Công ty có thể gặp những loại rủi ro tài chính nào?',
         dap_an='Rủi ro thị trường, rủi ro tín dụng và rủi ro thanh khoản.',
         bang_chung=[('rủi ro thị trường, rủi ro tín dụng và rủi ro thanh khoản',
                      ['CTR-BCTN-20251231', 'CTR-BCTC-HN-20251231-NAM', 'CTR-BCTC-R-20251231-NAM'],
                      ['CTR-BCTN-2025', 'CTR-BCTC-HN-2025', 'CTR-BCTC-R-2025'])]),
    dict(id='V08', nhom='rag_van_ban', bien_the=['cau_mo'],
         cau_hoi='Khẩu hiệu và định hướng trọng tâm năm 2025 mà HĐQT đề ra là gì?',
         dap_an='Khẩu hiệu "Vượt qua thử thách – Đẩy mạnh đầu tư – Phát triển bền vững"; trọng tâm: xây dựng chiến lược '
                'đến 2030, mở rộng kinh doanh ngoài Tập đoàn (trên 45% tổng doanh thu), Go Global 02-03 hợp đồng, '
                'đồng bộ và cho thuê 2.000 trạm BTS mới.',
         bang_chung=[('Khẩu hiệu năm 2025', ['CTR-BCTN-20241231'], [])]),
    dict(id='V09', nhom='rag_van_ban', bien_the=[],
         cau_hoi='Năm 2025 công ty quy hoạch bao nhiêu nhân tài, chia thành những nhóm nào?',
         dap_an='389 nhân tài (~3,75% tổng quân số): nhân tài chiến lược 23, chuyên gia 63, vận hành 303.',
         bang_chung=[('quy hoạch 389 nhân tài', ['CTR-BCTN-20251231'], [])]),
    dict(id='V10', nhom='rag_van_ban', bien_the=[],
         cau_hoi='Tổng tỷ lệ cổ tức năm 2022 công ty đã chi trả là bao nhiêu và chi trả thế nào?',
         dap_an='Tổng 29,19% bằng tiền: lần 1 tỷ lệ 10% (114,39 tỷ, trả 31/07/2023) và phần còn lại 19,19% bằng tiền '
                '(thay cho phương án trả bằng cổ phiếu 21,51% không được UBCK chấp thuận).',
         bang_chung=[('29,19%', ['CTR-NGHIQUYET-20240402'], ['CTR-NGHIQUYET', 'CTR-BAOCAODHDCD', 'CTR-TOTRINH', 'CTR-BCTN'])]),

    # ---------------- RAG: tra bảng (số không có trong danh mục chỉ tiêu BCTC) ----------------
    dict(id='B01', nhom='rag_bang', bien_the=['cach_goi_doi_thuong'],
         cau_hoi='Tập đoàn Viettel đang nắm bao nhiêu cổ phần của CTR?',
         dap_an='75.107.745 cổ phiếu, tương đương 65,66% (cơ cấu cổ đông trong BCTN 2025).',
         bang_chung=[('75.107.745', ['CTR-BCTN-20251231'], [])]),
    dict(id='B02', nhom='rag_bang', bien_the=['nam_gan'],
         cau_hoi='Đến cuối năm 2025 công ty sở hữu bao nhiêu trạm BTS?',
         dap_an='Khoảng 12.000 trạm BTS.',
         bang_chung=[('12.000 trạm', ['CTR-BCTN-20251231'], [r'CTR-\w+-202[56]'])],
         de_nham=[('lũy kế sở hữu ~ 10.000 trạm BTS', 'số của năm 2024 (báo cáo PTBV 2024)')]),
    dict(id='B03', nhom='rag_bang', bien_the=['thoi_gian_tuong_doi'],
         cau_hoi='Kế hoạch lợi nhuận sau thuế hợp nhất năm nay là bao nhiêu?',
         dap_an='Năm nay = 2026 (kỳ mới nhất trong kho 30/06/2026). Kế hoạch LNST hợp nhất 2026: 622,4 tỷ đồng '
                '(tổng doanh thu 15.653,4 tỷ, LNTT 778,0 tỷ).',
         bang_chung=[('622,4', ['CTR-KEHOACH-20260122'], ['CTR-KEHOACH-2026', 'CTR-NGHIQUYET-2026', 'CTR-TOTRINH-2026',
                                                          'CTR-BAOCAODHDCD-2026', 'CTR-TAILIEUHOP-2026'])]),
    dict(id='B04', nhom='rag_bang', bien_the=['viet_tat'],
         cau_hoi='ROE và ROA năm 2025 của CTR là bao nhiêu?',
         dap_an='ROE 30,5%, ROA 7,8% (BCTN 2025, mục Con số ấn tượng).',
         bang_chung=[('30,5%', ['CTR-BCTN-20251231'], ['CTR-BCTN-20251231'])]),
    dict(id='B05', nhom='rag_bang', bien_the=[],
         cau_hoi='Vốn hóa thị trường của cổ phiếu CTR tại ngày 31/12/2025 là bao nhiêu?',
         dap_an='9.666 tỷ đồng.',
         bang_chung=[('9.666 tỷ đồng', ['CTR-BCTN-20251231'], [])]),
    dict(id='B06', nhom='rag_bang', bien_the=[],
         cau_hoi='Năm 2024 công ty vận hành khai thác bao nhiêu trạm BTS và bao nhiêu thuê bao cố định băng rộng?',
         dap_an='Khoảng 58.420 trạm BTS và 7,7 triệu thuê bao CĐBR.',
         bang_chung=[('58.420 trạm BTS', ['CTR-BAOCAOPT-20241231'], [])]),
    dict(id='B07', nhom='rag_bang', bien_the=[],
         cau_hoi='Sau đợt phát hành cổ phiếu trả cổ tức năm 2025, vốn điều lệ của công ty là bao nhiêu?',
         dap_an='1.281.095.330.000 đồng (128.109.533 cổ phiếu), tăng từ 1.143.858.790.000 đồng do phát hành '
                '13.723.654 cổ phiếu trả cổ tức năm 2025.',
         bang_chung=[('1.281.095.330.000', ['CTR-CBTTPHATHANH-20260720'], [])]),
    dict(id='B08', nhom='rag_bang', bien_the=['viet_tat'],
         cau_hoi='KH tổng doanh thu và LNST hợp nhất năm 2025 theo NQ ĐHĐCĐ là bao nhiêu?',
         dap_an='Kế hoạch 2025 (hợp nhất): tổng doanh thu 13.968,0 tỷ, LNST 562,7 tỷ đồng.',
         bang_chung=[('13.968,0', ['CTR-NGHIQUYET-20250326'], [])]),
    dict(id='B09', nhom='rag_bang', bien_the=[],
         cau_hoi='Năm 2025 công ty thi công bao nhiêu căn nhà?',
         dap_an='1.720 căn nhà.',
         bang_chung=[('1.720 căn nhà', ['CTR-BCTN-20251231'], [])]),
    dict(id='B10', nhom='rag_bang', bien_the=['thoi_gian_tuong_doi'],
         cau_hoi='Mục tiêu ROE trong kế hoạch kinh doanh năm 2026 là bao nhiêu?',
         dap_an='ROE kế hoạch 2026: 26,3% (ROA 7,0%).',
         bang_chung=[('26,3%', ['CTR-KEHOACH-20260122'], ['CTR-KEHOACH-2026', 'CTR-NGHIQUYET-2026', 'CTR-TOTRINH-2026',
                                                          'CTR-BAOCAODHDCD-2026', 'CTR-TAILIEUHOP-2026'])]),

    # ---------------- RAG: nhiều trang / nhiều kỳ ----------------
    dict(id='M01', nhom='rag_nhieu_trang', bien_the=['so_sanh_ke_hoach'],
         cau_hoi='Năm 2025 công ty có hoàn thành kế hoạch lợi nhuận sau thuế hợp nhất không?',
         dap_an='Có. Kế hoạch 562,7 tỷ; thực hiện 599,72 tỷ (599.717.125.636 đồng) → vượt khoảng 37,0 tỷ (~6,6%).',
         bang_chung=[('562,7', ['CTR-NGHIQUYET-20250326'], ['CTR-NGHIQUYET-2025', 'CTR-BAOCAODHDCD-2025', 'CTR-TOTRINH-2025']),
                     ('599.717.125.636', ['CTR-BCTC-HN-20251231-NAM'], [])],
         can_du_moi_bang_chung=True),
    dict(id='M02', nhom='rag_nhieu_trang', bien_the=['nhieu_nam'],
         cau_hoi='Tỷ lệ cổ tức bằng tiền năm 2023 và năm 2024 khác nhau thế nào?',
         dap_an='Cổ tức 2023: 27,2% bằng tiền; cổ tức 2024: 21,5% bằng tiền → giảm 5,7 điểm phần trăm.',
         bang_chung=[('tỷ lệ 27,2%', ['CTR-NGHIQUYET-20250326'], []),
                     ('2.150 đồng', ['CTR-CBTTCOTUC-20250911'], ['CTR-CBTTCOTUC', 'CTR-NGHIQUYET'])],
         can_du_moi_bang_chung=True),
    dict(id='M03', nhom='rag_nhieu_trang', bien_the=['nhieu_ky', 'viet_tat'],
         cau_hoi='So sánh LNST hợp nhất quý 2/2024 với quý 3/2025.',
         dap_an='Quý II/2024: 125,1 tỷ; quý III/2025: 168,9 tỷ → cao hơn 43,8 tỷ.',
         bang_chung=[('125,1 tỷ', ['CTR-GIAITRINH-20240728'], ['CTR-GIAITRINH']),
                     ('168,9 tỷ', ['CTR-GIAITRINH-20251025'], ['CTR-GIAITRINH'])],
         can_du_moi_bang_chung=True),
    dict(id='M04', nhom='rag_nhieu_trang', bien_the=['nhieu_nam'],
         cau_hoi='Số trạm BTS công ty sở hữu tăng bao nhiêu từ cuối 2024 đến cuối 2025?',
         dap_an='Từ khoảng 10.000 trạm (cuối 2024) lên khoảng 12.000 trạm (cuối 2025): tăng khoảng 2.000 trạm.',
         bang_chung=[('lũy kế sở hữu ~ 10.000 trạm BTS', ['CTR-BAOCAOPT-20241231'], []),
                     ('12.000 trạm', ['CTR-BCTN-20251231'], [r'CTR-\w+-202[56]'])],
         can_du_moi_bang_chung=True),
    dict(id='M05', nhom='rag_nhieu_trang', bien_the=['nhieu_nam'],
         cau_hoi='Vốn điều lệ của công ty đã thay đổi thế nào từ năm 2022 đến nay?',
         dap_an='929.238.730.000 đồng → 1.143.858.790.000 đồng (năm 2022, chia cổ tức bằng cổ phiếu 23,1%) → '
                '1.281.095.330.000 đồng (2026, phát hành cổ phiếu trả cổ tức năm 2025).',
         bang_chung=[('929.238.730.000', ['CTR-NGHIQUYET-20230327'], ['CTR-NGHIQUYET', 'CTR-BAOCAODHDCD', 'CTR-TOTRINH']),
                     ('1.281.095.330.000', ['CTR-CBTTPHATHANH-20260720'], [])],
         can_du_moi_bang_chung=True),

    # ---------------- DB: một con số ----------------
    dict(id='D01', nhom='db_mot_so', bien_the=[],
         cau_hoi='Doanh thu thuần hợp nhất năm 2024 của CTR là bao nhiêu?',
         dap_an=dong(DT_HN_2024), dap_an_so=DT_HN_2024,
         chi_tieu=dict(ma_noi_bo='DOANH_THU_THUAN', tu_ngay='2024-01-01', den_ngay='2024-12-31', pham_vi='hop_nhat'),
         bang_chung=[('12.609.519.560.081', ['CTR-BCTC-HN-20241231-NAM'], [])],
         de_nham=[('11.370.408.272.724', 'doanh thu năm 2023 (cột năm trước, cùng trang)')]),
    dict(id='D02', nhom='db_mot_so', bien_the=['pham_vi_rieng', 'viet_tat'],
         cau_hoi='LNST trên báo cáo tài chính riêng năm 2025 là bao nhiêu?',
         dap_an=dong(587_417_356_381), dap_an_so=587_417_356_381,
         chi_tieu=dict(ma_noi_bo='LOI_NHUAN_SAU_THUE', tu_ngay='2025-01-01', den_ngay='2025-12-31', pham_vi='rieng'),
         bang_chung=[('587.417.356.381', ['CTR-BCTC-R-20251231-NAM'], [])],
         de_nham=[('599.717.125.636', 'LNST hợp nhất 2025 (sai phạm vi)')]),
    dict(id='D03', nhom='db_mot_so', bien_the=['thoi_gian_tuong_doi'],
         cau_hoi='Nợ phải trả hợp nhất tại kỳ báo cáo gần nhất là bao nhiêu?',
         dap_an='Kỳ gần nhất 30/06/2026: ' + dong(7_435_180_982_045), dap_an_so=7_435_180_982_045,
         chi_tieu=dict(ma_noi_bo='NO_PHAI_TRA', tu_ngay='2026-06-30', den_ngay='2026-06-30', pham_vi='hop_nhat'),
         bang_chung=[('7.435.180.982.045', ['CTR-BCTC-HN-20260630'], [])]),
    dict(id='D04', nhom='db_mot_so', bien_the=['viet_tat', 'cuoi_nam'],
         cau_hoi='HTK hợp nhất cuối năm 2025?',
         dap_an=dong(HTK_HN_2025) + ' (mã 140, đã trừ dự phòng)', dap_an_so=HTK_HN_2025,
         chi_tieu=dict(ma_noi_bo='HANG_TON_KHO_M140', tu_ngay='2025-12-31', den_ngay='2025-12-31', pham_vi='hop_nhat'),
         bang_chung=[('424.971.712.679', ['CTR-BCTC-HN-20251231-NAM'], [])]),
    dict(id='D05', nhom='db_mot_so', bien_the=['so_am', 'cach_goi_doi_thuong'],
         cau_hoi='Dòng tiền kinh doanh hợp nhất quý 1/2025 âm hay dương, bao nhiêu?',
         dap_an='Âm: lưu chuyển tiền thuần từ HĐKD 3 tháng đầu 2025 là -295.939.415.278 đồng.',
         dap_an_so=-295_939_415_278,
         chi_tieu=dict(ma_noi_bo='LCTT_THUAN_HDKD', tu_ngay='2025-01-01', den_ngay='2025-03-31', pham_vi='hop_nhat'),
         bang_chung=[('295.939.415.278', ['CTR-BCTC-HN-20250331-Q1'], [])]),
    dict(id='D06', nhom='db_mot_so', bien_the=['ban_nien', 'quy_vs_luy_ke'],
         cau_hoi='Lợi nhuận gộp hợp nhất 6 tháng đầu năm 2026 là bao nhiêu?',
         dap_an=dong(521_996_574_332), dap_an_so=521_996_574_332,
         chi_tieu=dict(ma_noi_bo='LOI_NHUAN_GOP', tu_ngay='2026-01-01', den_ngay='2026-06-30', pham_vi='hop_nhat'),
         bang_chung=[('521.996.574.332', ['CTR-BCTC-HN-20260630'], [])],
         de_nham=[('258.279.613.086', 'chỉ riêng quý 2/2026, không phải 6 tháng')]),
    dict(id='D07', nhom='db_mot_so', bien_the=['pham_vi_rieng', 'quy_vs_luy_ke'],
         cau_hoi='Doanh thu thuần riêng của quý 2/2026 (chỉ riêng quý, không luỹ kế) là bao nhiêu?',
         dap_an=dong(3_773_814_395_170), dap_an_so=3_773_814_395_170,
         chi_tieu=dict(ma_noi_bo='DOANH_THU_THUAN', tu_ngay='2026-04-01', den_ngay='2026-06-30', pham_vi='rieng'),
         bang_chung=[('3.773.814.395.170', ['CTR-BCTC-R-20260630-Q2'], [])],
         de_nham=[('7.146.665.879.548', 'luỹ kế 6 tháng'), ('4.154.012.949.194', 'số hợp nhất cùng quý')]),
    dict(id='D08', nhom='db_mot_so', bien_the=['bang_vat_trang', 'cach_goi_doi_thuong'],
         cau_hoi='Lãi ròng hợp nhất riêng quý 3/2024 là bao nhiêu?',
         dap_an=dong(146_262_730_079) + ' (chỉ quý III/2024)', dap_an_so=146_262_730_079,
         chi_tieu=dict(ma_noi_bo='LOI_NHUAN_SAU_THUE', tu_ngay='2024-07-01', den_ngay='2024-09-30', pham_vi='hop_nhat'),
         bang_chung=[('146.262.730.079', ['CTR-BCTC-HN-20240930-Q3'], [])],
         de_nham=[('140.605.159.668', 'quý III/2023 (cột cùng kỳ); trang 6 là trang nối tiếp không có dòng tiêu đề'),
                  ('387.502.476.388', 'luỹ kế 9 tháng 2024')],
         ghi_chu='Trang bảng vắt trang, không có tiêu đề cột. Bộ trích số hiện gán nhầm 140.605.159.668 thành '
                 'luỹ kế 9 tháng 2024 (lỗi đã ghi nhận 11/10/2026).'),

    # ---------------- DB: so sánh, tỷ lệ ----------------
    dict(id='C01', nhom='db_so_sanh', bien_the=['tang_truong'],
         cau_hoi='Doanh thu thuần hợp nhất năm 2025 tăng bao nhiêu phần trăm so với năm 2024?',
         dap_an=f'{ty(DT_HN_2025)} so với {ty(DT_HN_2024)}: tăng {pct(DT_HN_2025 - DT_HN_2024, DT_HN_2024)}.',
         dap_an_so=pct(DT_HN_2025 - DT_HN_2024, DT_HN_2024),
         bang_chung=[('13.939.517.269.142', ['CTR-BCTC-HN-20251231-NAM'], []),
                     ('12.609.519.560.081', ['CTR-BCTC-HN-20241231-NAM', 'CTR-BCTC-HN-20251231-NAM'], [])],
         can_du_moi_bang_chung=True),
    dict(id='C02', nhom='db_so_sanh', bien_the=['ty_le'],
         cau_hoi='Biên lợi nhuận gộp hợp nhất năm 2024 là bao nhiêu?',
         dap_an=f'Lợi nhuận gộp {ty(LNG_HN_2024)} / doanh thu thuần {ty(DT_HN_2024)} = {pct(LNG_HN_2024, DT_HN_2024)}.',
         dap_an_so=pct(LNG_HN_2024, DT_HN_2024),
         bang_chung=[('884.221.709.521', ['CTR-BCTC-HN-20241231-NAM'], []),
                     ('12.609.519.560.081', ['CTR-BCTC-HN-20241231-NAM'], [])],
         can_du_moi_bang_chung=True),
    dict(id='C03', nhom='db_so_sanh', bien_the=['ty_le', 'cuoi_nam'],
         cau_hoi='Tỷ lệ nợ phải trả trên tổng tài sản hợp nhất cuối năm 2025 là bao nhiêu?',
         dap_an=f'{ty(NPT_HN_2025)} / {ty(TTS_HN_2025)} = {pct(NPT_HN_2025, TTS_HN_2025)}.',
         dap_an_so=pct(NPT_HN_2025, TTS_HN_2025),
         bang_chung=[('6.118.368.595.239', ['CTR-BCTC-HN-20251231-NAM'], []),
                     ('8.185.544.854.669', ['CTR-BCTC-HN-20251231-NAM'], [])],
         can_du_moi_bang_chung=True),
    dict(id='C04', nhom='db_so_sanh', bien_the=['ban_nien', 'tang_truong'],
         cau_hoi='LNST hợp nhất 6 tháng đầu 2026 tăng bao nhiêu so với cùng kỳ năm trước?',
         dap_an=f'{ty(LNST_HN_6T2026)} so với {ty(LNST_HN_6T2025)}: tăng {ty(LNST_HN_6T2026 - LNST_HN_6T2025)} '
                f'({pct(LNST_HN_6T2026 - LNST_HN_6T2025, LNST_HN_6T2025)}).',
         dap_an_so=pct(LNST_HN_6T2026 - LNST_HN_6T2025, LNST_HN_6T2025),
         bang_chung=[('321.807.293.831', ['CTR-BCTC-HN-20260630'], []),
                     ('266.926.353.427', ['CTR-BCTC-HN-20250630', 'CTR-BCTC-HN-20260630'], [])],
         can_du_moi_bang_chung=True),
    dict(id='C05', nhom='db_so_sanh', bien_the=['ty_le'],
         cau_hoi='Năm 2025 dòng tiền thuần từ hoạt động kinh doanh hợp nhất gấp bao nhiêu lần lợi nhuận sau thuế?',
         dap_an=f'{ty(LCTT_HDKD_HN_2025)} / {ty(LNST_HN_2025)} ≈ '
                f'{str((Decimal(LCTT_HDKD_HN_2025) / Decimal(LNST_HN_2025)).quantize(Decimal("0.01"))).replace(".", ",")} lần.',
         dap_an_so=str((Decimal(LCTT_HDKD_HN_2025) / Decimal(LNST_HN_2025)).quantize(Decimal('0.01'))).replace('.', ','),
         bang_chung=[('1.455.415.537.450', ['CTR-BCTC-HN-20251231-NAM'], []),
                     ('599.717.125.636', ['CTR-BCTC-HN-20251231-NAM'], [])],
         can_du_moi_bang_chung=True),
    dict(id='C06', nhom='db_so_sanh', bien_the=['cuoi_nam', 'viet_tat'],
         cau_hoi='Hàng tồn kho hợp nhất cuối 2025 thay đổi thế nào so với cuối 2024?',
         dap_an=f'Giảm từ {ty(HTK_HN_2024)} xuống {ty(HTK_HN_2025)}: giảm {ty(HTK_HN_2024 - HTK_HN_2025)} '
                f'({pct(HTK_HN_2024 - HTK_HN_2025, HTK_HN_2024)}).',
         dap_an_so=pct(HTK_HN_2025 - HTK_HN_2024, HTK_HN_2024),
         bang_chung=[('424.971.712.679', ['CTR-BCTC-HN-20251231-NAM'], []),
                     ('610.480.085.129', ['CTR-BCTC-HN-20241231-NAM', 'CTR-BCTC-HN-20251231-NAM'], [])],
         can_du_moi_bang_chung=True),
    dict(id='C07', nhom='db_so_sanh', bien_the=['pham_vi_rieng', 'ty_le'],
         cau_hoi='Doanh thu thuần của công ty mẹ chiếm bao nhiêu phần trăm doanh thu thuần hợp nhất năm 2025?',
         dap_an=f'{ty(DT_R_2025)} / {ty(DT_HN_2025)} = {pct(DT_R_2025, DT_HN_2025)}.',
         dap_an_so=pct(DT_R_2025, DT_HN_2025),
         bang_chung=[('11.791.011.025.399', ['CTR-BCTC-R-20251231-NAM'], []),
                     ('13.939.517.269.142', ['CTR-BCTC-HN-20251231-NAM'], [])],
         can_du_moi_bang_chung=True),

    # ---------------- Ca biên của DB: số liệu trình bày lại ----------------
    dict(id='D09', nhom='db_mot_so', bien_the=['trinh_bay_lai'],
         cau_hoi='Doanh thu thuần hợp nhất năm 2019 (theo số liệu đã trình bày lại) là bao nhiêu?',
         dap_an='5.099.889.976.331 đồng (trình bày lại trong BCTC 2020; BCTC 2019 gốc ghi 5.053.710.724.827 đồng).',
         dap_an_so=5_099_889_976_331,
         chi_tieu=dict(ma_noi_bo='DOANH_THU_THUAN', tu_ngay='2019-01-01', den_ngay='2019-12-31', pham_vi='hop_nhat'),
         bang_chung=[('5.099.889.976.331', ['CTR-BCTC-HN-20201231-NAM'], [])],
         de_nham=[('5.053.710.724.827', 'số gốc của BCTC 2019, đã bị thay bằng số trình bày lại')]),

    dict(id='D10', nhom='db_mot_so', bien_the=['khong_dau'],
         cau_hoi='tong tai san hop nhat ngay 31/12/2024 la bao nhieu',
         dap_an=dong(7_106_257_863_668), dap_an_so=7_106_257_863_668,
         chi_tieu=dict(ma_noi_bo='TONG_TAI_SAN', tu_ngay='2024-12-31', den_ngay='2024-12-31', pham_vi='hop_nhat'),
         bang_chung=[('7.106.257.863.668', ['CTR-BCTC-HN-20241231-NAM'], [])]),
    dict(id='D11', nhom='db_mot_so', bien_the=['thoi_gian_tuong_doi', 'cach_goi_doi_thuong'],
         cau_hoi='Tiền mặt hợp nhất cuối quý trước là bao nhiêu?',
         dap_an='Quý trước = quý 1/2026 (kỳ mới nhất là quý 2/2026). Tiền và các khoản tương đương tiền hợp nhất '
                'tại 31/03/2026: ' + dong(1_123_253_891_417),
         dap_an_so=1_123_253_891_417,
         chi_tieu=dict(ma_noi_bo='TIEN_VA_TUONG_DUONG_TIEN', tu_ngay='2026-03-31', den_ngay='2026-03-31', pham_vi='hop_nhat'),
         bang_chung=[('1.123.253.891.417', ['CTR-BCTC-HN-20260331-Q1'], [])],
         de_nham=[('1.525.840.568.336', 'số tại 30/06/2026 (quý hiện tại, không phải quý trước)')]),
    dict(id='V11', nhom='rag_van_ban', bien_the=['nhan_su'],
         cau_hoi='Ai ký công văn giải trình kết quả kinh doanh quý 2/2024 gửi Ủy ban Chứng khoán?',
         dap_an='Ông Phạm Đình Trường, Tổng Giám đốc (công văn số 2507/VCC-TC ngày 28/07/2024).',
         bang_chung=[('Phạm Đình Trường', ['CTR-GIAITRINH-20240728'], ['CTR-GIAITRINH-20240728'])]),
    dict(id='X06', nhom='tu_choi', bien_the=['ky_chua_du', 'tra_loi_mot_phan'],
         cau_hoi='Doanh thu thuần hợp nhất cả năm 2026 là bao nhiêu?',
         dap_an='Chưa có số cả năm 2026 (kỳ mới nhất 30/06/2026). Có thể cung cấp số 6 tháng đầu 2026: '
                + dong(7_958_838_568_947) + '. Không được tự ngoại suy ra số cả năm.',
         bang_chung=[('7.958.838.568.947', ['CTR-BCTC-HN-20260630'], [])]),

    # ---------------- Phải từ chối ----------------
    dict(id='X01', nhom='tu_choi', bien_the=['cong_ty_khac'],
         cau_hoi='Doanh thu năm 2025 của FPT là bao nhiêu?',
         dap_an='Từ chối: kho chỉ có tài liệu của CTR (Viettel Construction).', bang_chung=[]),
    dict(id='X02', nhom='tu_choi', bien_the=['du_bao'],
         cau_hoi='Giá cổ phiếu CTR tuần tới sẽ tăng hay giảm?',
         dap_an='Từ chối: không dự báo giá cổ phiếu, ngoài phạm vi báo cáo đã công bố.', bang_chung=[]),
    dict(id='X03', nhom='tu_choi', bien_the=['ky_chua_co'],
         cau_hoi='Doanh thu thuần hợp nhất quý 3/2026 là bao nhiêu?',
         dap_an='Từ chối: kỳ mới nhất trong kho là 30/06/2026, chưa có báo cáo quý 3/2026.', bang_chung=[]),
    dict(id='X04', nhom='tu_choi', bien_the=['nam_ngoai_kho'],
         cau_hoi='Lợi nhuận sau thuế năm 2015 của CTR là bao nhiêu?',
         dap_an='Từ chối: kho không có báo cáo năm 2015 (dữ liệu từ năm 2019, số so sánh từ cuối 2017).', bang_chung=[]),
    dict(id='X05', nhom='tu_choi', bien_the=['khong_lien_quan'],
         cau_hoi='Thời tiết Hà Nội ngày mai thế nào?',
         dap_an='Từ chối: câu hỏi không liên quan đến báo cáo của công ty.', bang_chung=[]),
]


def build():
    pages, titles = load_pages()
    out, problems = [], []
    for item in Q:
        gold, groups = {}, {}
        for idx, (text, main, scope) in enumerate(item['bang_chung']):
            # một bằng chứng có thể có nhiều cách viết (danh sách): trang chứa một trong các cách viết là đủ
            alts = text if isinstance(text, (list, tuple)) else [text]
            hits = [(tid, n) for tid, n, body in pages
                    if (not scope or any(re.match(s, tid) for s in scope)) and any(a in body for a in alts)]
            if len(hits) > MAX_HITS:
                problems.append(f"{item['id']}: '{text}' khớp {len(hits)} trang, quá chung chung")
            if not any(tid.startswith(m) for tid, _ in hits for m in main):
                problems.append(f"{item['id']}: tài liệu chính {main} KHÔNG chứa '{text}'")
            for tid, n in hits[:MAX_HITS]:
                grade = 2 if any(tid.startswith(m) for m in main) else 1
                key = (tid, n)
                gold[key] = max(gold.get(key, 0), grade)
                groups.setdefault(key, set()).add(idx)
        negatives = []
        for text, note in item.get('de_nham', []):
            hits = [(tid, n) for tid, n, body in pages if text in body and (tid, n) not in gold]
            negatives += [dict(tai_lieu_id=tid, trang=n, ly_do=note) for tid, n in hits[:5]]
        entry = {k: item[k] for k in ('id', 'nhom', 'cau_hoi', 'bien_the', 'dap_an')}
        for k in ('dap_an_so', 'chi_tieu', 'can_du_moi_bang_chung', 'ghi_chu'):
            if k in item:
                entry[k] = item[k]
        entry['trang_vang'] = [dict(tai_lieu_id=tid, trang=n, muc=g, bang_chung_so=sorted(groups[(tid, n)]),
                                    ten=titles.get(tid, ''))
                               for (tid, n), g in sorted(gold.items(), key=lambda x: (-x[1], x[0]))]
        entry['bang_chung'] = [' || '.join(t) if isinstance(t, (list, tuple)) else t for t, _, _ in item['bang_chung']]
        if negatives:
            entry['trang_de_nham'] = negatives
        out.append(entry)

    counts = {}
    for e in out:
        counts[e['nhom']] = counts.get(e['nhom'], 0) + 1
    header = ('# Bộ câu hỏi đánh giá FinQuery — v1 (sinh bởi dung_bo_cau_hoi.py, 11/10/2026)\n'
              '# trang_vang.muc: 2 = trang chính chứa đáp án, 1 = trang khác cũng chứa đúng bằng chứng\n'
              '# can_du_moi_bang_chung: câu cần đủ mọi bằng chứng (nhiều trang/kỳ) mới trả lời được\n'
              '# trang_vang.bang_chung_so: trang chứa bằng chứng thứ mấy (chỉ số trong danh sách bang_chung, từ 0)\n'
              f'# Cơ cấu: {counts}\n')
    with open(OUT, 'w', encoding='utf-8') as f:
        f.write(header)
        yaml.safe_dump(out, f, allow_unicode=True, sort_keys=False, width=140)
    return out, problems, counts


if __name__ == '__main__':
    sys.stdout.reconfigure(encoding='utf-8')
    out, problems, counts = build()
    print('cơ cấu:', counts, 'tổng', len(out))
    for e in out:
        g2 = sum(1 for g in e['trang_vang'] if g['muc'] == 2)
        g1 = sum(1 for g in e['trang_vang'] if g['muc'] == 1)
        print(f"{e['id']:4} {e['nhom']:16} vàng: {g2} trang mức 2, {g1} trang mức 1, dễ nhầm {len(e.get('trang_de_nham', []))}")
    print('VẤN ĐỀ:' if problems else 'không có vấn đề')
    for p in problems:
        print('  -', p)
