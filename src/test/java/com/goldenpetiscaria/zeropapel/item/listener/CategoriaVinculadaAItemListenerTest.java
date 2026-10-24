package com.goldenpetiscaria.zeropapel.item.listener;

import com.goldenpetiscaria.zeropapel.categoria.evento.ExclusaoDeCategoriaSolicitadaEvent;
import com.goldenpetiscaria.zeropapel.common.exception.ConflitoException;
import com.goldenpetiscaria.zeropapel.item.repository.ItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaVinculadaAItemListenerTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private CategoriaVinculadaAItemListener listener;

    @Test
    void recusaExcluirCategoria_quandoHaItemVinculado() {
        when(itemRepository.existsByCategoriaId(5L)).thenReturn(true);

        assertThatThrownBy(() -> listener.aoSolicitarExclusaoDeCategoria(new ExclusaoDeCategoriaSolicitadaEvent(5L)))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("itens vinculados");
    }

    @Test
    void deixaExcluirCategoria_semItemVinculado() {
        when(itemRepository.existsByCategoriaId(5L)).thenReturn(false);

        assertThatCode(() -> listener.aoSolicitarExclusaoDeCategoria(new ExclusaoDeCategoriaSolicitadaEvent(5L)))
                .doesNotThrowAnyException();
    }
}
