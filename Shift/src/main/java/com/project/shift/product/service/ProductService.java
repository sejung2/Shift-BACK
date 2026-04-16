package com.project.shift.product.service;

import com.project.shift.product.dao.IImageDAO;
import com.project.shift.product.dao.ProductDAO;
import com.project.shift.product.dto.ImageDTO;
import com.project.shift.product.dto.ProductDTO;
import com.project.shift.product.entity.Image;
import com.project.shift.product.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * [SERVICE-001] 상품 관련 비즈니스 로직 처리 클래스
 * ---------------------------------------------------------
 * - PROD-001 : 전체 상품 목록 조회
 * - PROD-002 : 상품 상세 조회
 * - PROD-004 : 카테고리별 상품 조회
 * - PROD-005 : 상품 검색
 * - PROD-006 : 상품 정렬
 * - PROD-007 : 상품 이미지 조회
 * - PROD-014 : 상품 재고 조회
 * ---------------------------------------------------------
 * ※ 금액권(Category_ID = 3)은 모든 일반 조회/검색/정렬에서 제외
 */
@Service
@RequiredArgsConstructor
public class ProductService implements IProductService {

    private final ProductDAO productDAO;
    private final IImageDAO imageDAO;

    /** [PROD-001] 전체 상품 목록 조회 (금액권 제외) */
    @Override
    public List<ProductDTO> getAllProducts() {
        return productDAO.findAllExcludingCategory(3L)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /** [PROD-002] 상품 상세 조회 */
    @Override
    public ProductDTO getProductDetails(Long productId) {
        Product product = productDAO.findById(productId);
        if (product == null) {
            throw new IllegalArgumentException("[PROD-002] 상품을 찾을 수 없습니다. productId=" + productId);
        }
        return convertToDTO(product);
    }

    /** [PROD-004] 카테고리별 상품 목록 조회 */
    @Override
    public List<ProductDTO> getProductsByCategory(Long categoryId) {
        return productDAO.findByCategory(categoryId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /** [PROD-005] 상품 검색 (부분 일치, 금액권 제외) */
    @Override
    public List<ProductDTO> searchProducts(String keyword) {
        keyword = (keyword == null) ? "" : keyword.trim();
        if (keyword.isEmpty()) return List.of();
        final long EXCLUDED = 3L;
        return productDAO.searchByNameExcludingCategory(keyword, EXCLUDED)
                .stream()
                .distinct()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /** [PROD-006] 상품 정렬 (가격순 / 최신순, 금액권 제외) */
    @Override
    public List<ProductDTO> getSortedProducts(String sortType) {
        final String key = (sortType == null || sortType.isBlank()) ? "latest" : sortType.trim();
        final Sort sort = switch (key) {
            case "priceAsc"  -> Sort.by(Sort.Order.asc("price"), Sort.Order.asc("id"));
            case "priceDesc" -> Sort.by(Sort.Order.desc("price"), Sort.Order.asc("id"));
            default          -> Sort.by(Sort.Order.desc("registrationDate"), Sort.Order.desc("id"));
        };
        return productDAO.findAllSortedExcludingCategory(3L, sort)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /** [PROD-004 + PROD-006] 카테고리 한정 정렬 상품 목록 조회 */
    @Override
    public List<ProductDTO> getProductsByCategorySorted(Long categoryId, String sortType) {
        String key = (sortType == null || sortType.isBlank()) ? "latest" : sortType.trim();

        Sort sort = switch (key) {
            case "priceAsc"  -> Sort.by(Sort.Order.asc("price"), Sort.Order.asc("id"));
            case "priceDesc" -> Sort.by(Sort.Order.desc("price"), Sort.Order.asc("id"));
            default          -> Sort.by(Sort.Order.desc("registrationDate"), Sort.Order.desc("id"));
        };

        return productDAO.findByCategorySorted(categoryId, sort)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /** [PROD-007] 상품 이미지 목록 조회 */
    @Override
    public List<ImageDTO> getProductImages(Long productId) {
        return imageDAO.findByProductId(productId)
                .stream()
                .map(img -> ImageDTO.builder()
                        .imageId(img.getId())
                        .productId(img.getProduct().getId())
                        .imageUrl(img.getImageUrl())
                        .isRepresentative(img.getIsRepresentative())
                        .build())
                .collect(Collectors.toList());
    }

    /** [PROD-014] 상품 재고 조회 */
    @Override
    public ProductDTO getProductStock(Long productId) {
        Product product = productDAO.findById(productId);
        if (product == null) {
            throw new IllegalArgumentException("[PROD-009] 상품을 찾을 수 없습니다. productId=" + productId);
        }
        return new ProductDTO(product.getId().toString(), null, 0, product.getStock(), null, null, null, null);
    }

    /** 상품 저장 (시퀀스 기반 자동 ID 생성) */
    @Override
    public void saveProduct(Product product) {
        productDAO.saveProduct(product);
    }

    /** 엔티티 → DTO 변환 메서드 */
    private ProductDTO convertToDTO(Product product) {
        return ProductDTO.builder()
                .productId(product.getId().toString())
                .productName(product.getName())
                .price(product.getPrice())
                .stock(product.getStock())
                .registrationDate(product.getRegistrationDate().toString())
                .seller(product.getSeller())
                .categoryName(product.getCategory().getCategoryName())
                .imageUrls(product.getImages().stream()
                        .map(Image::getImageUrl)
                        .collect(Collectors.toList()))
                .build();
    }
}