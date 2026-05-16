package com.example.bookstore.service.recommendation;

import com.example.bookstore.config.RecommendationConfig;
import com.example.bookstore.model.Book;
import com.example.bookstore.model.Category;
import com.example.bookstore.model.User;
import com.example.bookstore.model.enums.ApprovalStatus;
import com.example.bookstore.model.enums.OrderStatus;
import com.example.bookstore.model.enums.UserRole;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.OrderItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationJobTest {

    @Mock
    private RecommendationConfig config;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private BookRepository bookRepository;

    private RecommendationCacheHolder cacheHolder;
    private RecommendationJob recommendationJob;

    @BeforeEach
    void setUp() {
        cacheHolder = new RecommendationCacheHolder();
        recommendationJob = new RecommendationJob();

        ReflectionTestUtils.setField(recommendationJob, "config", config);
        ReflectionTestUtils.setField(recommendationJob, "orderItemRepository", orderItemRepository);
        ReflectionTestUtils.setField(recommendationJob, "bookRepository", bookRepository);
        ReflectionTestUtils.setField(recommendationJob, "cacheHolder", cacheHolder);

        when(config.getMinSupport()).thenReturn(0.1);
        when(config.getMinConfidence()).thenReturn(0.1);
        when(config.getMinLift()).thenReturn(0.1);
        when(config.getMaxBoughtTogether()).thenReturn(5);
        when(config.getMaxSimilar()).thenReturn(5);
    }

    @Test
    void recompute_shouldSwapSnapshotAtomicallyWithoutMutatingPreviousSnapshot() {
        Book book10 = buildBook(10L, "Book 10", "Author A", 1L);
        Book book11 = buildBook(11L, "Book 11", "Author A", 1L);
        Book book12 = buildBook(12L, "Book 12", "Author A", 1L);

        when(orderItemRepository.findAllOrderBookPairsByStatuses(anyList()))
            .thenReturn(List.of(new Object[]{1L, 10L}, new Object[]{1L, 11L}));
        when(bookRepository.findByApprovalStatus(ApprovalStatus.APPROVED))
            .thenReturn(List.of(book10, book11));

        recommendationJob.recompute();
        RecommendationCache firstSnapshot = cacheHolder.get();

        assertThat(firstSnapshot.getBoughtTogether(10L)).contains(11L);

        when(orderItemRepository.findAllOrderBookPairsByStatuses(anyList()))
            .thenReturn(List.of(new Object[]{2L, 10L}, new Object[]{2L, 12L}));
        when(bookRepository.findByApprovalStatus(ApprovalStatus.APPROVED))
            .thenReturn(List.of(book10, book12));

        recommendationJob.recompute();
        RecommendationCache secondSnapshot = cacheHolder.get();

        assertThat(secondSnapshot).isNotSameAs(firstSnapshot);
        assertThat(secondSnapshot.getBoughtTogether(10L)).contains(12L);
        assertThat(firstSnapshot.getBoughtTogether(10L)).contains(11L);
        assertThat(firstSnapshot.getBoughtTogether(10L)).doesNotContain(12L);
    }

    private Book buildBook(Long id, String title, String author, Long categoryId) {
        Category category = new Category(categoryId, "Category " + categoryId, null, null);

        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setAuthor(author);
        book.setDescription("desc");
        book.setPrice(100000.0);
        book.setStockQuantity(10);
        book.setImageUrl("/img.png");
        book.setPublisher("NXB");
        book.setPublishYear(2024);
        book.setCategory(category);
        book.setSeller(User.builder().id(99L).username("seller").passwordHash("x").role(UserRole.SELLER).build());
        book.setApprovalStatus(ApprovalStatus.APPROVED);
        return book;
    }
}
