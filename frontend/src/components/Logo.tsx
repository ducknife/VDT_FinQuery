import logoFull from '../assets/images/logo-full.png'

// logo FinQuery (ảnh của người làm đề tài, đã bỏ nền trắng); dùng nguyên hình, không cắt vì hình và chữ đan vào nhau
export default function Logo({ size }: { size: number }) {
  return <img className="logo-img" src={logoFull} alt="FinQuery" style={{ height: size }} />
}
