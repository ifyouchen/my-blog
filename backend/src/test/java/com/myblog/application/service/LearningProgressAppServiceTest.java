package com.myblog.application.service;

import com.myblog.application.assembler.ArticleAssembler;
import com.myblog.application.dto.LearningProgressDTO;
import com.myblog.domain.model.valueobject.LearningPathArticle;
import com.myblog.domain.model.valueobject.TopicId;
import com.myblog.domain.repository.ArticleRepository;
import com.myblog.domain.repository.ColumnRepository;
import com.myblog.domain.repository.LearningProgressRepository;
import com.myblog.domain.repository.TopicRepository;
import com.myblog.domain.repository.UserRepository;
import com.myblog.infrastructure.repository.persistence.entity.LearningProgressDO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningProgressAppServiceTest {

    @Mock
    private LearningProgressRepository learningProgressRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private ColumnRepository columnRepository;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ArticleAssembler articleAssembler;

    private LearningProgressAppService service;

    @BeforeEach
    void setUp() {
        service = new LearningProgressAppService(
            learningProgressRepository,
            topicRepository,
            columnRepository,
            articleRepository,
            userRepository,
            articleAssembler
        );
    }

    @Test
    void updateProgressCreatesIdempotentTopicProgress() {
        List<LearningPathArticle> relations = Arrays.asList(
            new LearningPathArticle(101L, "基础", 1, true, ""),
            new LearningPathArticle(102L, "进阶", 2, true, "")
        );
        when(topicRepository.findArticleRelations(new TopicId(10L))).thenReturn(relations);
        when(learningProgressRepository.findByUserAndAsset(7L, "TOPIC", 10L)).thenReturn(null);
        when(articleRepository.findById(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.empty());

        LearningProgressDTO result = service.updateProgress(7L, "topic", 10L, 101L, true);

        ArgumentCaptor<LearningProgressDO> captor = ArgumentCaptor.forClass(LearningProgressDO.class);
        verify(learningProgressRepository).save(captor.capture());
        LearningProgressDO saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(7L);
        assertThat(saved.getAssetType()).isEqualTo("TOPIC");
        assertThat(saved.getAssetId()).isEqualTo(10L);
        assertThat(saved.getCompletedArticleIds()).isEqualTo("101");
        assertThat(saved.getCompletedCount()).isEqualTo(1);
        assertThat(saved.getProgressPercent()).isEqualTo(50);
        assertThat(result.getCompletedArticleIds()).containsExactly(101L);
        assertThat(result.getNextArticleId()).isEqualTo(102L);
    }
}
