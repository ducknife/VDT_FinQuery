package com.vdt.finq.modules.document;

import com.vdt.finq.modules.document.DocumentEnums.CheDo;
import com.vdt.finq.modules.document.DocumentEnums.Ky;
import com.vdt.finq.modules.document.DocumentEnums.LoaiTaiLieu;
import com.vdt.finq.modules.document.DocumentEnums.PhamVi;

import jakarta.persistence.AttributeConverter;

// JPA đọc ghi enum bằng value ("bctc"), không bằng tên hằng
public final class DocumentEnumConverters {

    private DocumentEnumConverters() {
    }

    public static class LoaiTaiLieuConverter implements AttributeConverter<LoaiTaiLieu, String> {
        @Override
        public String convertToDatabaseColumn(LoaiTaiLieu loai) {
            return loai == null ? null : loai.getValue();
        }

        @Override
        public LoaiTaiLieu convertToEntityAttribute(String value) {
            return LoaiTaiLieu.fromValue(value);
        }
    }

    public static class KyConverter implements AttributeConverter<Ky, String> {
        @Override
        public String convertToDatabaseColumn(Ky ky) {
            return ky == null ? null : ky.getValue();
        }

        @Override
        public Ky convertToEntityAttribute(String value) {
            return Ky.fromValue(value);
        }
    }

    public static class PhamViConverter implements AttributeConverter<PhamVi, String> {
        @Override
        public String convertToDatabaseColumn(PhamVi phamVi) {
            return phamVi == null ? null : phamVi.getValue();
        }

        @Override
        public PhamVi convertToEntityAttribute(String value) {
            return PhamVi.fromValue(value);
        }
    }

    public static class CheDoConverter implements AttributeConverter<CheDo, String> {
        @Override
        public String convertToDatabaseColumn(CheDo cheDo) {
            return cheDo == null ? null : cheDo.getValue();
        }

        @Override
        public CheDo convertToEntityAttribute(String value) {
            return CheDo.fromValue(value);
        }
    }
}
