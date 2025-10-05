package ahqpck.maintenance.report.controller;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ahqpck.maintenance.report.dto.BackupConfigDTO;
import ahqpck.maintenance.report.dto.BackupHistoryDTO;
import ahqpck.maintenance.report.entity.BackupConfig;
import ahqpck.maintenance.report.service.BackupConfigService;
import ahqpck.maintenance.report.service.BackupHistoryService;
import ahqpck.maintenance.report.service.ComplaintService;
import ahqpck.maintenance.report.repository.BackupConfigRepository;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/backup")
@RequiredArgsConstructor
public class BackupController {

    private final BackupConfigService backupConfigService;

    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    @GetMapping
    public String showBackup(Model model) {
        BackupConfigDTO dto = backupConfigService.getCurrentConfig();
        model.addAttribute("backupConfig", dto);
        model.addAttribute("title", "Backup Configuration");
        return "backup/config";
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    @PostMapping("/save-config")
    public String saveConfig(@ModelAttribute BackupConfigDTO dto) {
        backupConfigService.saveBackupConfig(dto);
        return "redirect:/backup";
    }

    @PreAuthorize("hasAnyRole('SUPERADMIN')")
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

    private final BackupHistoryService backupHistoryService;

    // @GetMapping
    // public String listBackups(
    //         @RequestParam(defaultValue = "1") int page,
    //         @RequestParam(defaultValue = "10") int size,
    //         @RequestParam(defaultValue = "backupDateTime") String sortBy,
    //         @RequestParam(defaultValue = "false") boolean asc,
    //         Model model) {

    //     try {
    //         int zeroBasedPage = page - 1;
    //         Page<BackupHistoryDTO> backupPage = backupHistoryService.getAllBackups(zeroBasedPage, size, sortBy, asc);
            
    //         model.addAttribute("backups", backupPage);
    //         model.addAttribute("currentPage", page);
    //         model.addAttribute("pageSize", size);
    //         model.addAttribute("sortBy", sortBy);
    //         model.addAttribute("asc", asc);
    //         model.addAttribute("title", "Backup History");
            
    //     } catch (Exception e) {
    //         model.addAttribute("error", "Failed to load backup history: " + e.getMessage());
    //         return "error/500";
    //     }

    //     return "backup/history"; // Adjust template path as needed
    // }

    @GetMapping("/delete/{id}")
    public String deleteBackup(@PathVariable Long id, RedirectAttributes ra) {
        try {
            backupHistoryService.deleteBackup(id);
            ra.addFlashAttribute("success", "Backup record deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/backup/history";
    }

}
