package com.myblog.growth.interfaces.rest.assembler;

import com.myblog.growth.domain.model.valueobject.ExpJournal;
import com.myblog.growth.domain.service.LevelPolicyService;
import com.myblog.growth.interfaces.rest.dto.response.ExpJournalVO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GrowthAssemblerTest {

    @Test
    void mapsActorExpJournalGrantRole() {
        GrowthAssembler assembler = new GrowthAssembler(mock(LevelPolicyService.class));
        ExpJournal journal = ExpJournal.create(
                1L, 2, 102, "COMMENT", 950011L,
                "ACTOR 行为经验", "COMMENT:ACTOR:1:950011:evt-1");

        ExpJournalVO vo = assembler.toVO(journal);

        assertThat(vo.getSourceId()).isEqualTo(950011L);
        assertThat(vo.getGrantRole()).isEqualTo("ACTOR");
        assertThat(vo.getGrantRoleLabel()).isEqualTo("你操作获得");
    }

    @Test
    void mapsAuthorExpJournalGrantRole() {
        GrowthAssembler assembler = new GrowthAssembler(mock(LevelPolicyService.class));
        ExpJournal journal = ExpJournal.create(
                1L, 2, 102, "LIKE", 950038L,
                "AUTHOR 行为经验", "LIKE:AUTHOR:1:950038:evt-2");

        ExpJournalVO vo = assembler.toVO(journal);

        assertThat(vo.getSourceId()).isEqualTo(950038L);
        assertThat(vo.getGrantRole()).isEqualTo("AUTHOR");
        assertThat(vo.getGrantRoleLabel()).isEqualTo("别人互动后你获得");
    }
}
