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
 * 
 * Colonnes attendues sur la première feuille : 0 Prénom, 1 Nom, 2 Email, 3 Téléphone.
 * Les lignes vides ou incomplètes sont ignorées.
 */
public final class EmployeeExcelUtility {

    private static final DataFormatter FORMATTER = new DataFormatter();//Formatter la date et l'heure

    private EmployeeExcelUtility() {
    }

    /**
     * @param inputStream flux Excel (.xlsx ou .xls)
     * @param companyId   identifiant de l’entreprise (injecté dans chaque DTO)
     * @return liste des salariés lus depuis le fichier
     */
    public static List<CreateEmployeeDTO> excelToEmployeeDtoList(InputStream inputStream, Long companyId) throws IOException {
        List<CreateEmployeeDTO> employees = new ArrayList<>();//Liste qui va contenir tous les employés lus depuis le fichier excel

        Workbook workbook = WorkbookFactory.create(inputStream);//Ouverture du fichier Excel
        try {
            Sheet sheet = workbook.getSheetAt(0);//Récupération de la première feuille

             //Parcours de chaque ligne de la feuille Excel
            for (Row row : sheet) {
                //Si la ligne est la première, on la saute
                if (row.getRowNum() == 0) {
                    continue;
                }

                 //Création d'un nouvel objet CreateEmployeeDTO
                CreateEmployeeDTO dto = new CreateEmployeeDTO();
                dto.setFirstName(getCellValue(row, 0));//Remplissage du prénom
                dto.setLastName(getCellValue(row, 1));//Remplissage du nom
                dto.setEmail(getCellValue(row, 2));//Remplissage de l'email
                dto.setPhone(getCellValue(row, 3));//Remplissage du téléphone
                dto.setAddress(getCellValue(row, 4));//Remplissage de l'adresse
                dto.setCompanyId(companyId);

                //Si la ligne est vide, on la saute
                if (isRowEmpty(dto)) {//Si la ligne est vide, on la saute
                    continue;
                }
                if (!isRowComplete(dto)) {//Si la ligne est incomplète, on la saute
                    continue;
                }
                employees.add(dto);//Ajout de l'objet CreateEmployeeDTO à la liste
            }
        } finally {
            workbook.close();//Fermeture du fichier Excel
        }

        return employees;//Retourne la liste des employés DTO lus depuis le fichier excel
    }

    //Récupération de la valeur d'une cellule
    private static String getCellValue(Row row, int columnIndex) {
        var cell = row.getCell(columnIndex);//Récupération de la cellule
        if (cell == null) {//Si la cellule est nulle, on retourne une chaîne vide
            return "";
        }
        return FORMATTER.formatCellValue(cell).trim();//Retourne la valeur de la cellule formatée
    }

    //Vérification si la ligne est vide
    private static boolean isRowEmpty(CreateEmployeeDTO dto) {
        return dto.getFirstName().isBlank()//Si le prénom est vide, on retourne true
                && dto.getLastName().isBlank()//Si le nom est vide, on retourne true
                && dto.getEmail().isBlank()//Si l'email est vide, on retourne true
                && dto.getPhone().isBlank();//Si le téléphone est vide, on retourne true
    }
    //Vérification si la ligne est complète
    private static boolean isRowComplete(CreateEmployeeDTO dto) {
        return !dto.getFirstName().isBlank()//Si le prénom est vide, on retourne false
                && !dto.getLastName().isBlank()//Si le nom est vide, on retourne false
                && !dto.getEmail().isBlank()//Si l'email est vide, on retourne false
                && !dto.getPhone().isBlank();//Si le téléphone est vide, on retourne false
    }
}
