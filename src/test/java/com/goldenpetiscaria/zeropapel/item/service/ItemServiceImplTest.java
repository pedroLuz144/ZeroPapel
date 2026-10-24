package com.goldenpetiscaria.zeropapel.item.service;

import com.goldenpetiscaria.zeropapel.categoria.service.CategoriaService;
import com.goldenpetiscaria.zeropapel.common.exception.ConflitoException;
import com.goldenpetiscaria.zeropapel.common.exception.RecursoNaoEncontradoException;
import com.goldenpetiscaria.zeropapel.item.entity.Item;
import com.goldenpetiscaria.zeropapel.item.evento.ExclusaoDeItemSolicitadaEvent;
import com.goldenpetiscaria.zeropapel.item.repository.ItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock
    private ItemRepository repository;

    @Mock
    private CategoriaService categoriaService;

    @Mock
    private ApplicationEventPublisher eventos;

    @InjectMocks
    private ItemServiceImpl service;

    @Test
    void excluirItemDoCardapio_publicaASolicitacaoDeExclusaoAntesDeExcluir() {
        Item porcao = item(7L);
        when(repository.findById(7L)).thenReturn(Optional.of(porcao));

        service.excluirItemDoCardapio(7L);

        InOrder ordem = inOrder(eventos, repository);
        ordem.verify(eventos).publishEvent(new ExclusaoDeItemSolicitadaEvent(7L));
        ordem.verify(repository).delete(porcao);
    }

    @Test
    void excluirItemDoCardapio_naoExclui_quandoUmOuvinteRecusa() {
        when(repository.findById(7L)).thenReturn(Optional.of(item(7L)));
        doThrow(new ConflitoException("pedidos vinculados"))
                .when(eventos).publishEvent(new ExclusaoDeItemSolicitadaEvent(7L));

        assertThatThrownBy(() -> service.excluirItemDoCardapio(7L))
                .isInstanceOf(ConflitoException.class);

        verify(repository, never()).delete(any());
    }

    @Test
    void excluirItemDoCardapio_lancaRecursoNaoEncontrado_semPublicar_quandoIdNaoExiste() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.excluirItemDoCardapio(7L))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(eventos, never()).publishEvent(any(Object.class));
        verify(repository, never()).delete(any());
    }

    private Item item(Long id) {
        Item item = new Item();
        item.setId(id);
        item.setNome("Porção de fritas");
        return item;
    }
}
