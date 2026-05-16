package com.myblog.growth.application.listener;

import com.myblog.application.event.CommentCreatedEvent;
import com.myblog.growth.application.event.BehaviorExpEvent;
import com.myblog.growth.application.service.ExpGrantAppService;
import com.myblog.growth.shared.enums.GrowthEventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GrantExpOnBehaviorEventHandlerTest {

    @Mock
    private ExpGrantAppService expGrantAppService;

    @Test
    void onCommentCreatedMapsCommentAuthorAsActorAndArticleAuthorAsAuthor() {
        GrantExpOnBehaviorEventHandler handler = new GrantExpOnBehaviorEventHandler(expGrantAppService);

        handler.onCommentCreated(new CommentCreatedEvent(1595L, 100L, 1L, 2L));

        ArgumentCaptor<BehaviorExpEvent> captor = ArgumentCaptor.forClass(BehaviorExpEvent.class);
        verify(expGrantAppService).grantExpForBehavior(captor.capture());
        BehaviorExpEvent event = captor.getValue();
        assertThat(event.getEventId()).isEqualTo("evt-COMMENT-v2-1-1595");
        assertThat(event.getEventType()).isEqualTo(GrowthEventType.COMMENT.name());
        assertThat(event.getActorUserId()).isEqualTo(1L);
        assertThat(event.getAuthorUserId()).isEqualTo(2L);
        assertThat(event.getSourceId()).isEqualTo(1595L);
    }
}
