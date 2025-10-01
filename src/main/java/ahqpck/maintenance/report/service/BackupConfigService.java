package ahqpck.maintenance.report.service;

import ahqpck.maintenance.report.dto.BackupConfigDTO;
import ahqpck.maintenance.report.entity.BackupConfig;
import ahqpck.maintenance.report.repository.*;
import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BackupConfigService {

    private final UserRepository userRepository;
    private final ComplaintRepository complaintRepository;
    private final WorkReportRepository workReportRepository;
    private final AreaRepository areaRepository;
    private final EquipmentRepository equipmentRepository;
    private final PartRepository partRepository;
    private final BackupConfigRepository backupConfigRepository;

    // --- SAVE CONFIGURATION ---
    @Transactional
    public void saveBackupConfig(BackupConfigDTO dto) {
        BackupConfig config = new BackupConfig();
        config.setIntervalDays(dto.getIntervalDays());
        config.setBackupTime(java.time.LocalTime.parse(dto.getBackupTime()));
        config.setStartDate(java.time.LocalDate.parse(dto.getStartDate()));
        config.setBackupFolder(dto.getBackupFolder());
        config.setBackupTypes(dto.getBackupTypes());
        config.setUpdatedAt(LocalDateTime.now());
        backupConfigRepository.save(config);
    }

    // --- BACKUP NOW ---
    public void backupNow(String backupFolder, Set<String> backupTypes) throws IOException {
        // Create folder if not exists
        Path folderPath = Paths.get(backupFolder);
        if (!Files.exists(folderPath)) {
            Files.createDirectories(folderPath);
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            if (backupTypes.contains("USER")) {
                createSheet(workbook, "Users", userRepository.findAll(), this::mapUser);
            }
            if (backupTypes.contains("AREA")) {
                createSheet(workbook, "Areas", areaRepository.findAll(), this::mapArea);
            }
            if (backupTypes.contains("EQUIPMENT")) {
                createSheet(workbook, "Equipments", equipmentRepository.findAll(), this::mapEquipment);
            }
            if (backupTypes.contains("PART")) {
                createSheet(workbook, "Parts", partRepository.findAll(), this::mapPart);
            }
            if (backupTypes.contains("COMPLAINT")) {
                createSheet(workbook, "Complaints", complaintRepository.findAll(), this::mapComplaint);
            }
            if (backupTypes.contains("WORK_REPORT")) {
                createSheet(workbook, "WorkReports", workReportRepository.findAll(), this::mapWorkReport);
            }

            // Save file
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "icbs_backup_" + timestamp + ".xlsx";
            Path filePath = folderPath.resolve(filename);
            try (FileOutputStream out = new FileOutputStream(filePath.toFile())) {
                workbook.write(out);
            }
        }
    }

    // --- GENERIC SHEET CREATOR ---
    private <T> void createSheet(Workbook workbook, String sheetName, List<T> entities, java.util.function.Function<T, Map<String, Object>> mapper) {
        if (entities.isEmpty()) return;

        Sheet sheet = workbook.createSheet(sheetName);
        List<Map<String, Object>> data = entities.stream().map(mapper).collect(Collectors.toList());

        // Headers
        Row headerRow = sheet.createRow(0);
        List<String> headers = new ArrayList<>(data.get(0).keySet());
        for (int i = 0; i < headers.size(); i++) {
            headerRow.createCell(i).setCellValue(headers.get(i));
        }

        // Data rows
        for (int i = 0; i < data.size(); i++) {
            Row row = sheet.createRow(i + 1);
            Map<String, Object> entityData = data.get(i);
            for (int j = 0; j < headers.size(); j++) {
                Object value = entityData.get(headers.get(j));
                if (value != null) {
                    row.createCell(j).setCellValue(value.toString());
                }
            }
        }
    }

    // ===================================================================
    // MAPPING FUNCTIONS (readable, at the bottom)
    // ===================================================================

    private Map<String, Object> mapUser(Object user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ID", getField(user, "id"));
        map.put("Name", getField(user, "name"));
        map.put("Employee ID", getField(user, "employeeId"));
        map.put("Email", getField(user, "email"));
        map.put("Role", getRoles(user));
        map.put("Phone", getField(user, "phoneNumber"));
        map.put("Designation", getField(user, "designation"));
        map.put("Join Date", getField(user, "joinDate"));
        map.put("Nationality", getField(user, "nationality"));
        return map;
    }

    private Map<String, Object> mapArea(Object area) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ID", getField(area, "id"));
        map.put("Code", getField(area, "code"));
        map.put("Name", getField(area, "name"));
        return map;
    }

    private Map<String, Object> mapEquipment(Object equipment) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ID", getField(equipment, "id"));
        map.put("Code", getField(equipment, "code"));
        map.put("Name", getField(equipment, "name"));
        map.put("Description", getField(equipment, "description"));
        map.put("Area Code", getNestedField(equipment, "area", "code"));
        map.put("Status", getField(equipment, "status"));
        map.put("Category", getField(equipment, "category"));
        return map;
    }

    private Map<String, Object> mapPart(Object part) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ID", getField(part, "id"));
        map.put("Code", getField(part, "code"));
        map.put("Name", getField(part, "name"));
        map.put("Description", getField(part, "description"));
        map.put("Category", getField(part, "category"));
        map.put("Unit", getField(part, "unit"));
        map.put("Min Stock", getField(part, "minStock"));
        return map;
    }

    private Map<String, Object> mapComplaint(Object complaint) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ID", getField(complaint, "id"));
        map.put("Code", getField(complaint, "code"));
        map.put("Title", getField(complaint, "title"));
        map.put("Description", getField(complaint, "description"));
        map.put("Reporter", getNestedField(complaint, "reporter", "name") + " (" + getNestedField(complaint, "reporter", "employeeId") + ")");
        map.put("Assignee", getNestedField(complaint, "assignee", "name") + " (" + getNestedField(complaint, "assignee", "employeeId") + ")");
        map.put("Area", getNestedField(complaint, "area", "name"));
        map.put("Equipment", getNestedField(complaint, "equipment", "name"));
        map.put("Status", getField(complaint, "status"));
        map.put("Priority", getField(complaint, "priority"));
        map.put("Created At", getField(complaint, "createdAt"));
        return map;
    }

    private Map<String, Object> mapWorkReport(Object workReport) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ID", getField(workReport, "id"));
        map.put("Code", getField(workReport, "code"));
        map.put("Report Date", getField(workReport, "reportDate"));
        map.put("Shift", getField(workReport, "shift"));
        map.put("Area", getNestedField(workReport, "area", "name") + " (" + getNestedField(workReport, "area", "code") + ")");
        map.put("Equipment", getNestedField(workReport, "equipment", "name") + " (" + getNestedField(workReport, "equipment", "code") + ")");
        map.put("Category", getField(workReport, "category"));
        map.put("Problem", getField(workReport, "problem"));
        map.put("Solution", getField(workReport, "solution"));
        map.put("Start Time", getField(workReport, "startTime"));
        map.put("Stop Time", getField(workReport, "stopTime"));
        map.put("Total Time Minutes", getField(workReport, "totalTimeMinutes"));
        map.put("Technicians", getTechnicians(workReport));
        map.put("Supervisor", getNestedField(workReport, "supervisor", "name") + " (" + getNestedField(workReport, "supervisor", "employeeId") + ")");
        map.put("Status", getField(workReport, "status"));
        map.put("Scope", getField(workReport, "scope"));
        map.put("Work Type", getField(workReport, "workType"));
        map.put("Remark", getField(workReport, "remark"));
        return map;
    }

    // ===================================================================
    // REFLECTION HELPERS (safe and simple)
    // ===================================================================

    private Object getField(Object entity, String fieldName) {
        try {
            java.lang.reflect.Field field = entity.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(entity);
            return value != null ? value : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String getNestedField(Object entity, String parentField, String childField) {
        try {
            java.lang.reflect.Field parent = entity.getClass().getDeclaredField(parentField);
            parent.setAccessible(true);
            Object parentObj = parent.get(entity);
            if (parentObj == null) return "";
            java.lang.reflect.Field child = parentObj.getClass().getDeclaredField(childField);
            child.setAccessible(true);
            Object value = child.get(parentObj);
            return value != null ? value.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String getRoles(Object user) {
        try {
            java.lang.reflect.Field rolesField = user.getClass().getDeclaredField("roles");
            rolesField.setAccessible(true);
            Set<?> roles = (Set<?>) rolesField.get(user);
            if (roles == null || roles.isEmpty()) return "";
            return roles.stream()
                    .map(role -> {
                        try {
                            java.lang.reflect.Field nameField = role.getClass().getDeclaredField("name");
                            nameField.setAccessible(true);
                            return nameField.get(role).toString();
                        } catch (Exception e) {
                            return role.toString();
                        }
                    })
                    .collect(Collectors.joining(", "));
        } catch (Exception e) {
            return "";
        }
    }

    private String getTechnicians(Object workReport) {
        try {
            java.lang.reflect.Field techsField = workReport.getClass().getDeclaredField("technicians");
            techsField.setAccessible(true);
            List<?> techs = (List<?>) techsField.get(workReport);
            if (techs == null || techs.isEmpty()) return "";
            return techs.stream()
                    .map(tech -> {
                        try {
                            String name = getField(tech, "name").toString();
                            String empId = getField(tech, "employeeId").toString();
                            return name + " (" + empId + ")";
                        } catch (Exception e) {
                            return tech.toString();
                        }
                    })
                    .collect(Collectors.joining(", "));
        } catch (Exception e) {
            return "";
        }
    }
}