package com.example.CalCol.service;

import com.example.CalCol.entity.Calculator;
import com.example.CalCol.entity.CalculatorImage;
import com.example.CalCol.entity.Label;
import com.example.CalCol.entity.UserCalculatorCollection;
import com.example.CalCol.entity.WishlistItem;
import com.example.CalCol.repository.CalculatorImageRepository;
import com.example.CalCol.repository.CalculatorLabelRepository;
import com.example.CalCol.repository.CalculatorLinkRepository;
import com.example.CalCol.repository.CalculatorRepository;
import com.example.CalCol.repository.LabelRepository;
import com.example.CalCol.repository.ManufacturerRepository;
import com.example.CalCol.repository.UserCalculatorCollectionRepository;
import com.example.CalCol.repository.WishlistItemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExportService {

	private final CalculatorRepository calculatorRepository;
	private final ManufacturerRepository manufacturerRepository;
	private final UserCalculatorCollectionRepository userCollectionRepository;
	private final WishlistItemRepository wishlistItemRepository;
	private final CalculatorLinkRepository linkRepository;
	private final CalculatorLabelRepository calculatorLabelRepository;
	private final LabelRepository labelRepository;
	private final CalculatorImageRepository calculatorImageRepository;
	private final FileStorageService fileStorageService;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public String exportAllDataAsJson() throws Exception {
		ObjectNode root = objectMapper.createObjectNode();
		
		// Export manufacturers
		ArrayNode manufacturers = root.putArray("manufacturers");
		manufacturerRepository.findAll().forEach(mfg -> {
			ObjectNode mfgNode = manufacturers.addObject();
			mfgNode.put("id", mfg.getId());
			mfgNode.put("name", mfg.getName());
		});

		// Export calculators
		ArrayNode calculators = root.putArray("calculators");
		calculatorRepository.findAll().forEach(calc -> {
			ObjectNode calcNode = calculators.addObject();
			calcNode.put("id", calc.getId());
			calcNode.put("model", calc.getModel());
			calcNode.put("manufacturerId", calc.getManufacturer().getId());
			if (calc.getSoldFrom() != null) calcNode.put("soldFrom", calc.getSoldFrom());
			if (calc.getSoldTo() != null) calcNode.put("soldTo", calc.getSoldTo());
			if (calc.getSourceUrl() != null) calcNode.put("sourceUrl", calc.getSourceUrl());
			if (calc.getRawRowText() != null) calcNode.put("rawRowText", calc.getRawRowText());
		});

		// Export labels
		ArrayNode labels = root.putArray("labels");
		labelRepository.findAll().forEach(label -> {
			ObjectNode labelNode = labels.addObject();
			labelNode.put("id", label.getId());
			labelNode.put("name", label.getName());
			labelNode.put("isCurated", label.getIsCurated());
			if (label.getDescription() != null) labelNode.put("description", label.getDescription());
		});

		// Export calculator labels
		ArrayNode calculatorLabels = root.putArray("calculatorLabels");
		calculatorLabelRepository.findAll().forEach(cl -> {
			ObjectNode clNode = calculatorLabels.addObject();
			clNode.put("calculatorId", cl.getCalculator().getId());
			clNode.put("labelId", cl.getLabel().getId());
		});

		return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
	}

	public String exportUserCollectionAsJson(String username) throws Exception {
		ObjectNode root = objectMapper.createObjectNode();
		root.put("username", username);
		root.put("exportDate", java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

		ArrayNode collection = root.putArray("collection");
		userCollectionRepository.findByUsernameOrderByAddedAtDesc(username, 
			org.springframework.data.domain.Pageable.unpaged())
			.getContent()
			.forEach(item -> {
				ObjectNode itemNode = collection.addObject();
				Calculator calc = item.getCalculator();
				itemNode.put("calculatorId", calc.getId());
				itemNode.put("model", calc.getModel());
				itemNode.put("manufacturer", calc.getManufacturer().getName());
				if (calc.getSoldFrom() != null) itemNode.put("soldFrom", calc.getSoldFrom());
				if (calc.getSoldTo() != null) itemNode.put("soldTo", calc.getSoldTo());
				itemNode.put("addedAt", item.getAddedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

				// Include labels
				ArrayNode labels = itemNode.putArray("labels");
				calculatorLabelRepository.findLabelsByCalculatorId(calc.getId())
					.forEach(label -> labels.add(label.getName()));

				// Include links
				ArrayNode links = itemNode.putArray("links");
				linkRepository.findByCalculatorId(calc.getId())
					.forEach(link -> {
						ObjectNode linkNode = links.addObject();
						linkNode.put("url", link.getUrl());
						linkNode.put("title", link.getTitle());
						if (link.getDescription() != null) linkNode.put("description", link.getDescription());
					});
			});

		return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
	}

	public String exportUserCollectionAsCsv(String username) {
		StringBuilder csv = new StringBuilder();
		csv.append("Calculator ID,Model,Manufacturer,Sold From,Sold To,Added At,Labels,Links\n");

		userCollectionRepository.findByUsernameOrderByAddedAtDesc(username, 
			org.springframework.data.domain.Pageable.unpaged())
			.getContent()
			.forEach(item -> {
				Calculator calc = item.getCalculator();
				csv.append(calc.getId()).append(",");
				csv.append(escapeCsv(calc.getModel())).append(",");
				csv.append(escapeCsv(calc.getManufacturer().getName())).append(",");
				csv.append(calc.getSoldFrom() != null ? calc.getSoldFrom() : "").append(",");
				csv.append(calc.getSoldTo() != null ? calc.getSoldTo() : "").append(",");
				csv.append(item.getAddedAt().format(DateTimeFormatter.ISO_LOCAL_DATE)).append(",");
				
				// Labels
				String labels = calculatorLabelRepository.findLabelsByCalculatorId(calc.getId())
					.stream()
					.map(Label::getName)
					.collect(Collectors.joining("; "));
				csv.append(escapeCsv(labels)).append(",");
				
				// Links
				String links = linkRepository.findByCalculatorId(calc.getId())
					.stream()
					.map(l -> l.getTitle() + " (" + l.getUrl() + ")")
					.collect(Collectors.joining("; "));
				csv.append(escapeCsv(links)).append("\n");
			});

		return csv.toString();
	}

	private String escapeCsv(String value) {
		if (value == null) return "";
		if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
			return "\"" + value.replace("\"", "\"\"") + "\"";
		}
		return value;
	}

	/**
	 * Export user collection as a mobile-friendly HTML file with embedded images
	 * Calculators are sorted by manufacturer, then by model
	 */
	public String exportUserCollectionAsHtml(String username) throws IOException {
		// Get all collection items
		List<UserCalculatorCollection> collectionItems = new java.util.ArrayList<>(
			userCollectionRepository
				.findByUsernameOrderByAddedAtDesc(username, org.springframework.data.domain.Pageable.unpaged())
				.getContent()
		);

		// Sort by manufacturer name, then by model
		collectionItems.sort(Comparator
			.comparing((UserCalculatorCollection item) -> item.getCalculator().getManufacturer().getName())
			.thenComparing(item -> item.getCalculator().getModel()));

		StringBuilder html = new StringBuilder();
		html.append("<!DOCTYPE html>\n");
		html.append("<html lang=\"en\">\n");
		html.append("<head>\n");
		html.append("  <meta charset=\"UTF-8\">\n");
		html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
		html.append("  <title>Calculator Collection - ").append(escapeHtml(username)).append("</title>\n");
		html.append("  <style>\n");
		html.append("    * { margin: 0; padding: 0; box-sizing: border-box; }\n");
		html.append("    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif; background: #f5f5f5; padding: 20px; line-height: 1.6; }\n");
		html.append("    .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 10px; margin-bottom: 30px; text-align: center; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }\n");
		html.append("    .header h1 { font-size: 28px; margin-bottom: 10px; }\n");
		html.append("    .header p { font-size: 16px; opacity: 0.9; }\n");
		html.append("    .collection { display: flex; flex-direction: column; gap: 30px; }\n");
		html.append("    .manufacturer-section { margin-bottom: 30px; }\n");
		html.append("    .manufacturer-calculators { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; }\n");
		html.append("    @media (max-width: 600px) { .manufacturer-calculators { grid-template-columns: 1fr; } }\n");
		html.append("    .calculator-card { background: white; border-radius: 10px; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); transition: transform 0.2s, box-shadow 0.2s; }\n");
		html.append("    .calculator-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.15); }\n");
		html.append("    .manufacturer { font-size: 14px; color: #667eea; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 8px; }\n");
		html.append("    .model { font-size: 22px; font-weight: 700; color: #333; margin-bottom: 15px; }\n");
		html.append("    .image-container { width: 100%; margin-bottom: 15px; text-align: center; }\n");
		html.append("    .calculator-image { max-width: 100%; height: auto; border-radius: 8px; box-shadow: 0 2px 6px rgba(0,0,0,0.1); }\n");
		html.append("    .no-image { color: #999; font-style: italic; padding: 20px; background: #f9f9f9; border-radius: 8px; }\n");
		html.append("    .section-divider { margin: 40px 0 20px; border: none; border-top: 2px solid #e0e0e0; }\n");
		html.append("    .manufacturer-header { font-size: 24px; font-weight: 700; color: #667eea; margin: 30px 0 15px; padding-bottom: 10px; border-bottom: 2px solid #667eea; }\n");
		html.append("  </style>\n");
		html.append("</head>\n");
		html.append("<body>\n");
		html.append("  <div class=\"header\">\n");
		html.append("    <h1>Calculator Collection</h1>\n");
		html.append("    <p>").append(escapeHtml(username)).append(" • ").append(collectionItems.size()).append(" calculator(s)</p>\n");
		html.append("    <p style=\"font-size: 14px; margin-top: 10px;\">Exported on ").append(java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("</p>\n");
		html.append("  </div>\n");
		html.append("  <div class=\"collection\">\n");

		String currentManufacturer = null;
		boolean manufacturerSectionOpen = false;
		
		for (UserCalculatorCollection item : collectionItems) {
			Calculator calc = item.getCalculator();
			String manufacturer = calc.getManufacturer().getName();
			
			// Add manufacturer header when manufacturer changes
			if (!manufacturer.equals(currentManufacturer)) {
				if (manufacturerSectionOpen) {
					html.append("    </div>\n"); // Close previous manufacturer section's grid
					html.append("  </div>\n"); // Close previous manufacturer section
				}
				html.append("  <div class=\"manufacturer-section\">\n");
				html.append("    <h2 class=\"manufacturer-header\">").append(escapeHtml(manufacturer)).append("</h2>\n");
				html.append("    <div class=\"manufacturer-calculators\">\n"); // Grid for calculators in this manufacturer
				currentManufacturer = manufacturer;
				manufacturerSectionOpen = true;
			}

			html.append("      <div class=\"calculator-card\">\n");
			html.append("        <div class=\"manufacturer\">").append(escapeHtml(manufacturer)).append("</div>\n");
			html.append("        <div class=\"model\">").append(escapeHtml(calc.getModel())).append("</div>\n");
			
			// Get approved images for this calculator
			List<CalculatorImage> images = calculatorImageRepository.findByCalculatorIdAndIsApprovedTrue(calc.getId());
			if (!images.isEmpty()) {
				// Use the first approved image
				CalculatorImage image = images.get(0);
				try {
					Path imagePath = fileStorageService.loadFile(image.getImagePath());
					if (Files.exists(imagePath)) {
						byte[] imageBytes = Files.readAllBytes(imagePath);
						String base64Image = Base64.getEncoder().encodeToString(imageBytes);
						
						// Determine MIME type from file extension
						String mimeType = "image/jpeg";
						String filename = image.getImagePath().toLowerCase();
						if (filename.endsWith(".png")) {
							mimeType = "image/png";
						} else if (filename.endsWith(".gif")) {
							mimeType = "image/gif";
						} else if (filename.endsWith(".webp")) {
							mimeType = "image/webp";
						}
						
						html.append("        <div class=\"image-container\">\n");
						html.append("          <img src=\"data:").append(mimeType).append(";base64,").append(base64Image).append("\" alt=\"").append(escapeHtml(calc.getModel())).append("\" class=\"calculator-image\">\n");
						html.append("        </div>\n");
					} else {
						html.append("        <div class=\"image-container\">\n");
						html.append("          <div class=\"no-image\">No image available</div>\n");
						html.append("        </div>\n");
					}
				} catch (Exception e) {
					html.append("        <div class=\"image-container\">\n");
					html.append("          <div class=\"no-image\">No image available</div>\n");
					html.append("        </div>\n");
				}
			} else {
				html.append("        <div class=\"image-container\">\n");
				html.append("          <div class=\"no-image\">No image available</div>\n");
				html.append("        </div>\n");
			}
			
			html.append("      </div>\n");
		}
		
		if (manufacturerSectionOpen) {
			html.append("    </div>\n"); // Close last manufacturer section's grid
			html.append("  </div>\n"); // Close last manufacturer section
		}
		
		html.append("  </div>\n");
		html.append("</body>\n");
		html.append("</html>\n");
		
		return html.toString();
	}

	public String exportUserWishlistAsJson(String username) throws Exception {
		ObjectNode root = objectMapper.createObjectNode();
		root.put("username", username);
		root.put("exportDate", java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

		ArrayNode wishlist = root.putArray("wishlist");
		getSortedWishlistItems(username).forEach(item -> {
			ObjectNode itemNode = wishlist.addObject();
			Calculator calc = item.getCalculator();
			itemNode.put("calculatorId", calc.getId());
			itemNode.put("model", calc.getModel());
			itemNode.put("manufacturer", calc.getManufacturer().getName());
			if (calc.getSoldFrom() != null) itemNode.put("soldFrom", calc.getSoldFrom());
			if (calc.getSoldTo() != null) itemNode.put("soldTo", calc.getSoldTo());
			itemNode.put("addedAt", item.getAddedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
			if (item.getNotes() != null) itemNode.put("notes", item.getNotes());
			if (item.getMarktplaatsQuery() != null) itemNode.put("marktplaatsQuery", item.getMarktplaatsQuery());
			if (item.getEbayQuery() != null) itemNode.put("ebayQuery", item.getEbayQuery());
			if (item.getEtsyQuery() != null) itemNode.put("etsyQuery", item.getEtsyQuery());
		});

		return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
	}

	public String exportUserWishlistAsCsv(String username) {
		StringBuilder csv = new StringBuilder();
		csv.append("Calculator ID,Model,Manufacturer,Sold From,Sold To,Added At,Notes,Marktplaats Query,eBay Query,Etsy Query\n");

		getSortedWishlistItems(username).forEach(item -> {
			Calculator calc = item.getCalculator();
			csv.append(calc.getId()).append(",");
			csv.append(escapeCsv(calc.getModel())).append(",");
			csv.append(escapeCsv(calc.getManufacturer().getName())).append(",");
			csv.append(calc.getSoldFrom() != null ? calc.getSoldFrom() : "").append(",");
			csv.append(calc.getSoldTo() != null ? calc.getSoldTo() : "").append(",");
			csv.append(item.getAddedAt().format(DateTimeFormatter.ISO_LOCAL_DATE)).append(",");
			csv.append(escapeCsv(item.getNotes())).append(",");
			csv.append(escapeCsv(item.getMarktplaatsQuery())).append(",");
			csv.append(escapeCsv(item.getEbayQuery())).append(",");
			csv.append(escapeCsv(item.getEtsyQuery())).append("\n");
		});

		return csv.toString();
	}

	public String exportUserWishlistAsHtml(String username) throws IOException {
		List<WishlistItem> items = getSortedWishlistItems(username);

		StringBuilder html = new StringBuilder();
		html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
		html.append("  <meta charset=\"UTF-8\">\n");
		html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
		html.append("  <title>Calculator Wishlist - ").append(escapeHtml(username)).append("</title>\n");
		html.append("  <style>\n");
		html.append("    * { margin: 0; padding: 0; box-sizing: border-box; }\n");
		html.append("    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f5f5f5; padding: 20px; line-height: 1.5; color: #333; }\n");
		html.append("    .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 24px; border-radius: 10px; margin-bottom: 24px; text-align: center; }\n");
		html.append("    .header h1 { font-size: 26px; margin-bottom: 8px; }\n");
		html.append("    table { width: 100%; border-collapse: collapse; background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }\n");
		html.append("    th, td { padding: 12px 14px; text-align: left; border-bottom: 1px solid #eee; vertical-align: top; }\n");
		html.append("    th { background: #667eea; color: white; font-size: 13px; text-transform: uppercase; letter-spacing: 0.4px; }\n");
		html.append("    tr:nth-child(even) { background: #fafafa; }\n");
		html.append("    .model { font-weight: 700; }\n");
		html.append("    .manufacturer { color: #667eea; font-weight: 600; }\n");
		html.append("    .notes { color: #555; font-size: 14px; }\n");
		html.append("    .query a { color: #667eea; word-break: break-word; }\n");
		html.append("    @media print { body { background: white; padding: 0; } .header { border-radius: 0; } table { box-shadow: none; } }\n");
		html.append("    @media (max-width: 900px) { table, thead, tbody, th, td, tr { display: block; } thead { display: none; } tr { margin-bottom: 16px; border: 1px solid #eee; border-radius: 8px; padding: 8px; } td { border: none; padding: 6px 10px; } td::before { content: attr(data-label); font-weight: 700; display: block; margin-bottom: 4px; color: #667eea; font-size: 12px; text-transform: uppercase; } }\n");
		html.append("  </style>\n</head>\n<body>\n");
		html.append("  <div class=\"header\">\n");
		html.append("    <h1>Calculator Wishlist</h1>\n");
		html.append("    <p>").append(escapeHtml(username)).append(" &bull; ").append(items.size()).append(" calculator(s)</p>\n");
		html.append("    <p style=\"font-size: 14px; margin-top: 8px;\">Exported on ")
			.append(java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("</p>\n");
		html.append("  </div>\n");

		if (items.isEmpty()) {
			html.append("  <p>Your wishlist is empty.</p>\n");
		} else {
			html.append("  <table>\n    <thead><tr>\n");
			html.append("      <th>#</th><th>Manufacturer</th><th>Model</th><th>Years</th><th>Notes</th><th>Marktplaats</th><th>eBay</th><th>Etsy</th>\n");
			html.append("    </tr></thead>\n    <tbody>\n");
			int index = 1;
			for (WishlistItem item : items) {
				Calculator calc = item.getCalculator();
				html.append("    <tr>\n");
				html.append("      <td data-label=\"#\">").append(index++).append("</td>\n");
				html.append("      <td data-label=\"Manufacturer\" class=\"manufacturer\">").append(escapeHtml(calc.getManufacturer().getName())).append("</td>\n");
				html.append("      <td data-label=\"Model\" class=\"model\">").append(escapeHtml(calc.getModel())).append("</td>\n");
				html.append("      <td data-label=\"Years\">").append(formatYears(calc)).append("</td>\n");
				html.append("      <td data-label=\"Notes\" class=\"notes\">").append(escapeHtml(nullToEmpty(item.getNotes()))).append("</td>\n");
				html.append("      <td data-label=\"Marktplaats\" class=\"query\">").append(formatSearchLink("marktplaats", item.getMarktplaatsQuery())).append("</td>\n");
				html.append("      <td data-label=\"eBay\" class=\"query\">").append(formatSearchLink("ebay", item.getEbayQuery())).append("</td>\n");
				html.append("      <td data-label=\"Etsy\" class=\"query\">").append(formatSearchLink("etsy", item.getEtsyQuery())).append("</td>\n");
				html.append("    </tr>\n");
			}
			html.append("    </tbody>\n  </table>\n");
		}

		html.append("</body>\n</html>\n");
		return html.toString();
	}

	private List<WishlistItem> getSortedWishlistItems(String username) {
		List<WishlistItem> items = new java.util.ArrayList<>(
			wishlistItemRepository.findByUsernameOrderByAddedAtDesc(username, org.springframework.data.domain.Pageable.unpaged())
				.getContent()
		);
		items.sort(Comparator
			.comparing((WishlistItem item) -> item.getCalculator().getManufacturer().getName())
			.thenComparing(item -> item.getCalculator().getModel()));
		return items;
	}

	private String formatYears(Calculator calc) {
		if (calc.getSoldFrom() == null && calc.getSoldTo() == null) {
			return "";
		}
		if (calc.getSoldFrom() != null && calc.getSoldTo() != null) {
			return escapeHtml(calc.getSoldFrom() + " - " + calc.getSoldTo());
		}
		Integer year = calc.getSoldFrom() != null ? calc.getSoldFrom() : calc.getSoldTo();
		return escapeHtml(String.valueOf(year));
	}

	private String formatSearchLink(String platform, String query) {
		if (query == null || query.isBlank()) {
			return "";
		}
		String encoded = java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
		String url = switch (platform) {
			case "marktplaats" -> "https://www.marktplaats.nl/q/" + encoded + "/";
			case "ebay" -> "https://www.ebay.nl/sch/i.html?_nkw=" + encoded;
			case "etsy" -> "https://www.etsy.com/nl/search?q=" + encoded;
			default -> "";
		};
		return "<a href=\"" + escapeHtml(url) + "\" target=\"_blank\" rel=\"noopener\">" + escapeHtml(query) + "</a>";
	}

	private String nullToEmpty(String value) {
		return value != null ? value : "";
	}

	private String escapeHtml(String text) {
		if (text == null) return "";
		return text.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\"", "&quot;")
			.replace("'", "&#39;");
	}
}

