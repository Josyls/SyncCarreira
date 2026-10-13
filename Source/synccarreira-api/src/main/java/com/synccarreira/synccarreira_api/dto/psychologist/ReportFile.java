package com.synccarreira.synccarreira_api.dto.psychologist;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Arquivo de relatório pronto para download
@AllArgsConstructor
@Getter
public class ReportFile {

    public static final String XLSX_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final String fileName;

    private final byte[] content;
}
