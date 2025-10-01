package ahqpck.maintenance.report.controller;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ahqpck.maintenance.report.dto.BackupConfigDTO;
import ahqpck.maintenance.report.entity.BackupConfig;
import ahqpck.maintenance.report.service.BackupConfigService;
import ahqpck.maintenance.report.service.ComplaintService;
import ahqpck.maintenance.report.repository.BackupConfigRepository;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/backup")
@RequiredArgsConstructor
public class BackupConfigController {

    private final BackupConfigService backupConfigService;
    private final BackupConfigRepository backupConfigRepository;

    @GetMapping
    public String showBackup(Model model) {
        BackupConfig config = backupConfigRepository.findTopByOrderByIdDesc()
                .orElse(new BackupConfig());

        BackupConfigDTO dto = new BackupConfigDTO();

        // Null-safe binding
        dto.setIntervalDays(config.getIntervalDays() != null ? config.getIntervalDays() : 7);
        dto.setBackupTime(config.getBackupTime() != null
                ? config.getBackupTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                : "");
        dto.setStartDate(config.getStartDate() != null
                ? config.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                : "");
        dto.setBackupFolder(config.getBackupFolder() != null
                ? config.getBackupFolder()
                : "");
        dto.setBackupTypes(config.getBackupTypes() != null
                ? config.getBackupTypes()
                : "");

                System.out.println("Backup Types get: " + dto.getBackupTypes());
        model.addAttribute("backupConfig", dto);
        model.addAttribute("title", "Backup Configuration");
        return "backup/index";
    }

    // Save config
    @PostMapping("/save-config")
    public String saveConfig(@ModelAttribute BackupConfigDTO dto) {
        System.out.println("Backup Types save: " + dto.getBackupTypes());
        backupConfigService.saveBackupConfig(dto);
        return "redirect:/backup";
    }

    // Backup now
    @PostMapping("/backup-now")
    public String backupNow(@ModelAttribute BackupConfigDTO dto) {
        try {
            Set<String> types = Arrays.stream(dto.getBackupTypes().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
            backupConfigService.backupNow(dto.getBackupFolder(), types);
        } catch (Exception e) {
            // handle error
        }
        return "redirect:/backup";
    }
}
