package com.example.CalCol.controller;

import com.example.CalCol.entity.Calculator;
import com.example.CalCol.entity.Label;
import com.example.CalCol.service.AdminService;
import com.example.CalCol.service.CalculatorProposalService;
import com.example.CalCol.service.ExportService;
import com.example.CalCol.service.ImageService;
import com.example.CalCol.service.ImportService;
import com.example.CalCol.service.LabelDerivationService;
import com.example.CalCol.service.LabelService;
import com.example.CalCol.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

	private final AdminService adminService;
	private final ImageService imageService;
	private final LabelService labelService;
	private final LabelDerivationService labelDerivationService;
	private final ExportService exportService;
	private final ImportService importService;
	private final CalculatorProposalService proposalService;
	private final UserService userService;
	private static final int PAGE_SIZE = 20;

	@GetMapping("/dashboard")
	public String dashboard(
			@RequestParam(defaultValue = "0") int page,
			Model model) {
		Pageable pageable = PageRequest.of(page, PAGE_SIZE);
		model.addAttribute("proposals", imageService.getPendingProposals(pageable));
		return "admin/dashboard";
	}

	@GetMapping("/calculators")
	public String manageCalculators(
			@RequestParam(required = false) String search,
			@RequestParam(defaultValue = "0") int page,
			Model model) {
		Pageable pageable = PageRequest.of(page, PAGE_SIZE);
		model.addAttribute("calculators", adminService.getAllCalculators(pageable));
		if (search != null && !search.trim().isEmpty()) {
			model.addAttribute("search", search);
		}
		return "admin/calculators";
	}

	@GetMapping("/calculators/new")
	public String newCalculatorForm(Model model) {
		model.addAttribute("calculator", new Calculator());
		model.addAttribute("manufacturers", adminService.getAllManufacturers(Pageable.unpaged()).getContent());
		return "admin/calculator-form";
	}

	@GetMapping("/calculators/edit/{id}")
	public String editCalculatorForm(@PathVariable Long id, Model model) {
		adminService.getCalculatorById(id).ifPresent(calc -> {
			model.addAttribute("calculator", calc);
			model.addAttribute("manufacturers", adminService.getAllManufacturers(Pageable.unpaged()).getContent());
		});
		return "admin/calculator-form";
	}

	@PostMapping("/calculators")
	public String saveCalculator(
			@ModelAttribute Calculator calculator,
			@RequestParam(required = false) Long manufacturerId,
			RedirectAttributes redirectAttributes) {
		if (manufacturerId != null) {
			adminService.getManufacturerById(manufacturerId).ifPresent(calculator::setManufacturer);
		}

		adminService.createCalculator(calculator);
		redirectAttributes.addFlashAttribute("successMessage", "Calculator created successfully!");
		return "redirect:/admin/calculators";
	}

	@PostMapping("/calculators/{id}")
	public String updateCalculator(
			@PathVariable Long id,
			@ModelAttribute Calculator calculator,
			@RequestParam(required = false) Long manufacturerId,
			RedirectAttributes redirectAttributes) {
		if (manufacturerId != null) {
			adminService.getManufacturerById(manufacturerId).ifPresent(calculator::setManufacturer);
		}

		adminService.updateCalculator(id, calculator);
		redirectAttributes.addFlashAttribute("successMessage", "Calculator updated successfully!");
		return "redirect:/admin/calculators";
	}

	@PostMapping("/calculators/{id}/delete")
	public String deleteCalculator(
			@PathVariable Long id,
			RedirectAttributes redirectAttributes) {
		if (adminService.deleteCalculator(id)) {
			redirectAttributes.addFlashAttribute("successMessage", "Calculator deleted successfully!");
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete calculator.");
		}
		return "redirect:/admin/calculators";
	}

	@PostMapping("/proposals/images/{imageId}/approve")
	public String approveImageProposal(
			@PathVariable Long imageId,
			Authentication authentication,
			RedirectAttributes redirectAttributes) {
		if (imageService.approveImage(imageId, authentication.getName())) {
			redirectAttributes.addFlashAttribute("successMessage", "Image proposal approved!");
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", "Failed to approve proposal.");
		}
		return "redirect:/admin/dashboard";
	}

	@PostMapping("/proposals/images/{imageId}/reject")
	public String rejectImageProposal(
			@PathVariable Long imageId,
			RedirectAttributes redirectAttributes) {
		if (imageService.rejectImage(imageId)) {
			redirectAttributes.addFlashAttribute("successMessage", "Image proposal rejected.");
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", "Failed to reject proposal.");
		}
		return "redirect:/admin/dashboard";
	}

	@GetMapping("/labels")
	public String manageLabels(Model model) {
		model.addAttribute("labels", labelService.getAllLabels());
		return "admin/labels";
	}

	@GetMapping("/labels/new")
	public String newLabelForm(Model model) {
		model.addAttribute("label", new Label());
		return "admin/label-form";
	}

	@GetMapping("/labels/edit/{id}")
	public String editLabelForm(@PathVariable Long id, Model model) {
		labelService.getLabelById(id).ifPresent(label -> {
			model.addAttribute("label", label);
		});
		return "admin/label-form";
	}

	@PostMapping("/labels")
	public String saveLabel(
			@ModelAttribute Label label,
			@RequestParam(required = false) String description,
			@RequestParam(value = "isCurated", defaultValue = "false") boolean isCurated,
			RedirectAttributes redirectAttributes) {
		label.setDescription(description);
		label.setIsCurated(isCurated);
		labelService.createCuratedLabel(label.getName(), label.getDescription());
		redirectAttributes.addFlashAttribute("successMessage", "Label created successfully!");
		return "redirect:/admin/labels";
	}

	@PostMapping("/labels/{id}")
	public String updateLabel(
			@PathVariable Long id,
			@RequestParam String name,
			@RequestParam(required = false) String description,
			@RequestParam(value = "isCurated", defaultValue = "false") boolean isCurated,
			RedirectAttributes redirectAttributes) {
		try {
			labelService.updateLabel(id, name, description, isCurated);
			redirectAttributes.addFlashAttribute("successMessage", "Label updated successfully!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", "Failed to update label: " + e.getMessage());
		}
		return "redirect:/admin/labels";
	}

	@PostMapping("/labels/{id}/delete")
	public String deleteLabel(
			@PathVariable Long id,
			RedirectAttributes redirectAttributes) {
		if (labelService.deleteLabel(id)) {
			redirectAttributes.addFlashAttribute("successMessage", "Label deleted successfully!");
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete label.");
		}
		return "redirect:/admin/labels";
	}

	@PostMapping("/labels/derive")
	public String deriveLabels(RedirectAttributes redirectAttributes) {
		try {
			int assigned = labelDerivationService.deriveLabelsFromCalculators();
			redirectAttributes.addFlashAttribute("successMessage", 
				"Label derivation completed! Assigned " + assigned + " labels to calculators.");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", 
				"Failed to derive labels: " + e.getMessage());
		}
		return "redirect:/admin/labels";
	}

	@GetMapping("/export")
	public org.springframework.http.ResponseEntity<String> exportAllData() {
		try {
			String jsonData = exportService.exportAllDataAsJson();
			String filename = "calculator_collector_export_" + java.time.LocalDate.now() + ".json";
			return org.springframework.http.ResponseEntity.ok()
				.header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
				.contentType(org.springframework.http.MediaType.APPLICATION_JSON)
				.body(jsonData);
		} catch (Exception e) {
			return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@PostMapping("/import")
	public String importAllData(
			@RequestParam("file") org.springframework.web.multipart.MultipartFile file,
			RedirectAttributes redirectAttributes) {
		if (file.isEmpty()) {
			redirectAttributes.addFlashAttribute("errorMessage", "Please select a file to import.");
			return "redirect:/admin/dashboard";
		}

		try {
			String jsonData = new String(file.getBytes());
			ImportService.ImportResult result = importService.importAllData(jsonData);
			redirectAttributes.addFlashAttribute("successMessage", 
				"Import completed! Created: " + result.manufacturersCreated + " manufacturers, " +
				result.calculatorsCreated + " calculators, " + result.labelsCreated + " labels.");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", 
				"Failed to import data: " + e.getMessage());
		}

		return "redirect:/admin/dashboard";
	}

	@GetMapping("/proposals")
	public String manageProposals(
			@RequestParam(defaultValue = "0") int page,
			Model model) {
		Pageable pageable = PageRequest.of(page, PAGE_SIZE);
		model.addAttribute("proposals", proposalService.getPendingProposals(pageable));
		return "admin/calculator-proposals";
	}

	@PostMapping("/proposals/{id}/approve")
	public String approveCalculatorProposal(
			@PathVariable Long id,
			Authentication authentication,
			RedirectAttributes redirectAttributes) {
		if (proposalService.approveProposal(id, authentication.getName())) {
			redirectAttributes.addFlashAttribute("successMessage", "Calculator proposal approved and added to database!");
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", "Failed to approve proposal.");
		}
		return "redirect:/admin/proposals";
	}

	@PostMapping("/proposals/{id}/reject")
	public String rejectCalculatorProposal(
			@PathVariable Long id,
			RedirectAttributes redirectAttributes) {
		if (proposalService.rejectProposal(id)) {
			redirectAttributes.addFlashAttribute("successMessage", "Calculator proposal rejected.");
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", "Failed to reject proposal.");
		}
		return "redirect:/admin/proposals";
	}

	@GetMapping("/users")
	public String manageUsers(
			@RequestParam(defaultValue = "0") int page,
			Model model) {
		Pageable pageable = PageRequest.of(page, PAGE_SIZE);
		model.addAttribute("users", userService.getAllUsers(pageable));
		return "admin/users";
	}

	@GetMapping("/users/new")
	public String newUserForm(Model model) {
		model.addAttribute("user", new com.example.CalCol.entity.AppUser());
		return "admin/user-form";
	}

	@GetMapping("/users/edit/{id}")
	public String editUserForm(@PathVariable Long id, Model model) {
		userService.getUserById(id).ifPresent(user -> {
			model.addAttribute("user", user);
		});
		return "admin/user-form";
	}

	@PostMapping("/users")
	public String saveUser(
			@RequestParam String username,
			@RequestParam String email,
			@RequestParam String password,
			@RequestParam String role,
			@RequestParam(value = "enabled", defaultValue = "true") boolean enabled,
			RedirectAttributes redirectAttributes) {
		try {
			userService.createUser(username, email, password, role);
			redirectAttributes.addFlashAttribute("successMessage", "User created successfully!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", "Failed to create user: " + e.getMessage());
		}
		return "redirect:/admin/users";
	}

	@PostMapping("/users/{id}")
	public String updateUser(
			@PathVariable Long id,
			@RequestParam String email,
			@RequestParam String role,
			@RequestParam(value = "enabled", defaultValue = "true") boolean enabled,
			RedirectAttributes redirectAttributes) {
		try {
			userService.updateUser(id, email, role, enabled);
			redirectAttributes.addFlashAttribute("successMessage", "User updated successfully!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", "Failed to update user: " + e.getMessage());
		}
		return "redirect:/admin/users";
	}

	@PostMapping("/users/{id}/delete")
	public String deleteUser(
			@PathVariable Long id,
			RedirectAttributes redirectAttributes) {
		if (userService.deleteUser(id)) {
			redirectAttributes.addFlashAttribute("successMessage", "User deleted successfully!");
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete user.");
		}
		return "redirect:/admin/users";
	}
}

