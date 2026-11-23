package com.pnn.backend.service;

import com.pnn.backend.domain.EvidenceReadingCache;
import com.pnn.backend.repository.EvidenceReadingCacheRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EvidenceReadingCacheServiceTest {

    private final EvidenceReadingCacheRepository repository = mock(EvidenceReadingCacheRepository.class); // 가짜 DB
    private final EvidenceReadingCacheService service = new EvidenceReadingCacheService(repository, "test-v1");

    @Test
    @DisplayName("문장 지문: 띄어쓰기·줄바꿈만 다른 문장은 같은 지문, 내용이 다르면 다른 지문")
    void hashIgnoresSpacing() {
        String a = "5) 철분제제와 병용투여시 흡수가 감소될 수 있다.";
        assertThat(EvidenceReadingCacheService.hash(a)).isEqualTo(EvidenceReadingCacheService.hash("5)  철분제제와\n병용투여시 흡수가 감소될 수 있다. "));
        assertThat(EvidenceReadingCacheService.hash(a)).isNotEqualTo(EvidenceReadingCacheService.hash("5) 칼슘제제와 병용투여시 흡수가 감소될 수 있다."));
        assertThat(EvidenceReadingCacheService.hash(a)).hasSize(64);
    }

    @Test
    @DisplayName("찾기: 지금 버전으로 저장된 결과를 '문장|성분' 열쇠로 돌려준다")
    void findReturnsRowsBySentenceAndGroup() {
        String sentence = "5) 철분제제와 병용투여시 흡수가 감소될 수 있다.";
        EvidenceReadingCache row = new EvidenceReadingCache();
        row.setSentenceHash(EvidenceReadingCacheService.hash(sentence));
        row.setNutrientGroup("철분");
        when(repository.findBySentenceHashInAndCacheVersion(anyCollection(), eq("test-v1"))).thenReturn(List.of(row));

        Map<String, EvidenceReadingCache> found = service.find(List.of(sentence));

        assertThat(found).containsOnlyKeys(sentence + "|철분");
    }

    @Test
    @DisplayName("저장: 지문·버전·원문을 함께 저장한다")
    void saveStoresKeyAndSentence() {
        String sentence = "5) 철분제제와 병용투여시 흡수가 감소될 수 있다.";

        service.save(sentence, "철분", "CAUTION", "철분제는 흡수를 줄여요.", null, null);

        ArgumentCaptor<EvidenceReadingCache> saved = ArgumentCaptor.forClass(EvidenceReadingCache.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getSentenceHash()).isEqualTo(EvidenceReadingCacheService.hash(sentence));
        assertThat(saved.getValue().getCacheVersion()).isEqualTo("test-v1");
        assertThat(saved.getValue().getSentence()).isEqualTo(sentence);
        assertThat(saved.getValue().getCreatedAt()).isNotNull();
    }
}
