package com.example.coopachat.util;

import com.example.coopachat.dtos.employees.CreateEmployeeDTO;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Lit un fichier Excel et transforme chaque ligne (sauf l’en-tête) en {@link CreateEmployeeDTO}.
 * <p>
 * Colonnes attendues sur la première feuille : 0 Prénom, 1 Nom, 2 Email, 3 Téléphone, 4 Adresse.
 * Les lignes vides ou incomplètes sont ignorées.
 */
public final class EmployeeExcelUtility {

    private static final DataFormatter FORMATTER = new DataFormatter();

    private EmployeeExcelUtility() {
    }

    /**
     * @param inputStream flux Excel (.xlsx ou .xls)
     * @param companyId   identifiant de l’entreprise (injecté dans chaque DTO)
     * @return liste des salariés lus depuis le fichier
     */
    public static List<CreateEmployeeDTO> excelToEmployeeDtoList(InputStream inputStream, Long companyId) throws IOException {
        List<CreateEmployeeDTO> employees = new ArrayList<>();

        Workbook workbook = WorkbookFactory.create(inputStream);
        try {
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0) {
                    continue;
                }

                CreateEmployeeDTO dto = new CreateEmployeeDTO();
                dto.setFirstName(getCellValue(row, 0));
                dto.setLastName(getCellValue(row, 1));
                dto.setEmail(getCellValue(row, 2));
                dto.setPhone(getCellValue(row, 3));
                dto.setAddress(getCellValue(row, 4));
                dto.setCompanyId(companyId);

                if (isRowEmpty(dto)) {
                    continue;
                }
                if (!isRowComplete(dto)) {
                    continue;
                }

                employees.add(dto);
            }
        } finally {
            workbook.close();
        }

        return employees;
    }

    private static String getCellValue(Row row, int columnIndex) {
        var cell = row.getCell(columnIndex);
        if (cell == null) {
            return "";
        }
        return FORMATTER.formatCellValue(cell).trim();
    }

    private static boolean isRowEmpty(CreateEmployeeDTO dto) {
        return dto.getFirstName().isBlank()
                && dto.getLastName().isBlank()
                && dto.getEmail().isBlank()
                && dto.getPhone().isBlank()
                && dto.getAddress().isBlank();
    }

    private static boolean isRowComplete(CreateEmployeeDTO dto) {
        return !dto.getFirstName().isBlank()
                && !dto.getLastName().isBlank()
                && !dto.getEmail().isBlank()
                && !dto.getPhone().isBlank()
                && !dto.getAddress().isBlank();
    }
}
