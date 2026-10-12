"""Sinh data/eval/eval_dbquery.yml: bộ câu hỏi nhánh số liệu (18 câu DB + 3 câu từ chối có số),
lấy từ eval_test.yml và thêm phần `cham` để máy chấm được (đúng và đủ số được hỏi, như paper FinAI).

cham.kieu:
  so_dong    số tiền: câu trả lời phải có số đồng chính xác HOẶC số tỷ đồng làm tròn 2 chữ số;
             am = true thì phải nói rõ là âm ("âm", "-", hoặc trong ngoặc)
  phan_tram  phải có đúng chuỗi phần trăm (vd "10,55%"); huong = tang/giam nếu câu hỏi về thay đổi
  lan        phải có đúng số lần (vd "2,43")
  tu_choi    không được đưa ra số cho kỳ được hỏi; so_duoc_phep = các số được phép nhắc (vd số 6 tháng)

Chạy:  python dung_bo_cau_hoi_db.py
"""
import yaml

SRC = 'eval_test.yml'
OUT = 'eval_dbquery.yml'

# chấm cho các câu so sánh / tỷ lệ / từ chối (câu một số lấy từ dap_an_so)
CHAM = {
    'C01': {'kieu': 'phan_tram', 'gia_tri': '10,55', 'huong': 'tang'},
    'C02': {'kieu': 'phan_tram', 'gia_tri': '7,01'},
    'C03': {'kieu': 'phan_tram', 'gia_tri': '74,75'},
    'C04': {'kieu': 'phan_tram', 'gia_tri': '20,56', 'huong': 'tang'},
    'C05': {'kieu': 'lan', 'gia_tri': '2,43'},
    'C06': {'kieu': 'phan_tram', 'gia_tri': '30,39', 'huong': 'giam'},
    'C07': {'kieu': 'phan_tram', 'gia_tri': '84,59'},
    'X03': {'kieu': 'tu_choi', 'so_duoc_phep': []},
    'X04': {'kieu': 'tu_choi', 'so_duoc_phep': []},
    'X06': {'kieu': 'tu_choi', 'so_duoc_phep': [7958838568947]},
}
TU_CHOI = ('X03', 'X04', 'X06')


def main():
    qs = yaml.safe_load(open(SRC, encoding='utf-8'))
    out = []
    for q in qs:
        if not (q['nhom'].startswith('db') or q['id'] in TU_CHOI):
            continue
        if q['id'] in CHAM:
            cham = dict(CHAM[q['id']])
        else:
            v = int(q['dap_an_so'])
            cham = {'kieu': 'so_dong', 'gia_tri': v}
            if v < 0:
                cham['am'] = True
        item = {'id': q['id'], 'nhom': 'tu_choi' if q['id'] in TU_CHOI else q['nhom'],
                'cau_hoi': q['cau_hoi'], 'bien_the': q.get('bien_the', []), 'dap_an': q['dap_an'], 'cham': cham}
        if q.get('chi_tieu'):
            item['chi_tieu'] = q['chi_tieu']
        out.append(item)
    header = ('# Bộ câu hỏi nhánh số liệu (sinh bởi dung_bo_cau_hoi_db.py từ eval_test.yml): '
              f'{len(out)} câu.\n# cham: cách máy chấm "đúng và đủ" (xem docstring của script).\n')
    with open(OUT, 'w', encoding='utf-8') as f:
        f.write(header)
        yaml.safe_dump(out, f, allow_unicode=True, sort_keys=False, width=200)
    print(f'đã ghi {OUT}: {len(out)} câu')


if __name__ == '__main__':
    main()
