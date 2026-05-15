package com.goldenpetiscaria.zeropapel.domain.service.implementacoes;

import com.goldenpetiscaria.zeropapel.domain.entity.FechamentoCaixa;
import com.goldenpetiscaria.zeropapel.domain.entity.ItemPedido;
import com.goldenpetiscaria.zeropapel.domain.entity.Pedido;
import com.goldenpetiscaria.zeropapel.domain.entity.Usuario;
import com.goldenpetiscaria.zeropapel.domain.service.contratos.FechamentoService;
import com.goldenpetiscaria.zeropapel.dto.request.RealizarFechamentoRequest;
import com.goldenpetiscaria.zeropapel.dto.response.*;
import com.goldenpetiscaria.zeropapel.infra.exception.ConflitoException;
import com.goldenpetiscaria.zeropapel.infra.exception.RecursoNaoEncontradoException;
import com.goldenpetiscaria.zeropapel.repository.FechamentoCaixaRepository;
import com.goldenpetiscaria.zeropapel.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class FechamentoServiceImpl implements FechamentoService {

    private final FechamentoCaixaRepository fechamentoRepository;
    private final PedidoRepository pedidoRepository;

    public FechamentoServiceImpl(FechamentoCaixaRepository fechamentoRepository,
                                 PedidoRepository pedidoRepository) {
        this.fechamentoRepository = fechamentoRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Override
    @Transactional
    public FechamentoResponseDTO realizarFechamento(RealizarFechamentoRequest request, Usuario usuario) {
        if (fechamentoRepository.countByPeriodoSobreposto(request.de(), request.ate()) > 0) {
            throw new ConflitoException("Já existe um fechamento para esse período.");
        }

        List<Pedido> pedidos = pedidoRepository.findByPeriodoComItens(request.de(), request.ate());
        FechamentoResponseDTO calculado = calcularFechamento(request.de(), request.ate(), pedidos);

        FechamentoCaixa fechamento = new FechamentoCaixa();
        fechamento.setDe(request.de());
        fechamento.setAte(request.ate());
        fechamento.setGeradoEm(LocalDateTime.now());
        fechamento.setGeradoPor(usuario);
        fechamento.setTotalPedidos(calculado.resumo().totalPedidos());
        fechamento.setFaturamentoBruto(calculado.resumo().faturamentoBruto());
        fechamento.setTotalTaxas(calculado.resumo().totalTaxas());
        fechamento.setFaturamentoLiquido(calculado.resumo().faturamentoLiquido());
        fechamento.setTicketMedio(calculado.resumo().ticketMedio());

        FechamentoCaixa salvo = fechamentoRepository.save(fechamento);

        return new FechamentoResponseDTO(
                salvo.getId(),
                calculado.de(),
                calculado.ate(),
                calculado.resumo(),
                calculado.porPlataforma(),
                calculado.porFormaDePagamento(),
                calculado.itensMaisVendidos(),
                calculado.pedidosPorHora()
        );
    }

    @Override
    public FechamentoResponseDTO buscarFechamentoPorId(Long id) {
        FechamentoCaixa fechamento = fechamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fechamento não encontrado para o ID informado"));

        // Resumo usa os totais congelados no momento do fechamento
        ResumoFechamentoDTO resumo = new ResumoFechamentoDTO(
                fechamento.getTotalPedidos(),
                fechamento.getFaturamentoBruto(),
                fechamento.getTotalTaxas(),
                fechamento.getFaturamentoLiquido(),
                fechamento.getTicketMedio()
        );

        // Breakdowns recalculados a partir dos pedidos do período
        List<Pedido> pedidos = pedidoRepository.findByPeriodoComItens(fechamento.getDe(), fechamento.getAte());
        FechamentoResponseDTO breakdowns = calcularFechamento(fechamento.getDe(), fechamento.getAte(), pedidos);

        return new FechamentoResponseDTO(
                fechamento.getId(),
                fechamento.getDe(),
                fechamento.getAte(),
                resumo,
                breakdowns.porPlataforma(),
                breakdowns.porFormaDePagamento(),
                breakdowns.itensMaisVendidos(),
                breakdowns.pedidosPorHora()
        );
    }

    @Override
    public List<FechamentoCaixaListagemDTO> listarFechamentos() {
        List<FechamentoCaixa> fechamentos = fechamentoRepository.findAll();
        List<FechamentoCaixaListagemDTO> resultado = new ArrayList<>();

        for (FechamentoCaixa fechamento : fechamentos) {
            resultado.add(new FechamentoCaixaListagemDTO(
                    fechamento.getId(),
                    fechamento.getDe(),
                    fechamento.getAte(),
                    fechamento.getGeradoEm(),
                    fechamento.getGeradoPor().getNome(),
                    fechamento.getTotalPedidos(),
                    fechamento.getFaturamentoBruto(),
                    fechamento.getTotalTaxas(),
                    fechamento.getFaturamentoLiquido(),
                    fechamento.getTicketMedio()
            ));
        }

        // Ordena do mais recente para o mais antigo
        resultado.sort((fechamentoA, fechamentoB) -> fechamentoB.geradoEm().compareTo(fechamentoA.geradoEm()));

        return resultado;
    }

    private FechamentoResponseDTO calcularFechamento(LocalDateTime de, LocalDateTime ate, List<Pedido> pedidos) {
        ResumoFechamentoDTO resumo                        = calcularResumo(pedidos);
        List<FechamentoPorPlataformaDTO>       porPlataforma       = calcularPorPlataforma(pedidos);
        List<FechamentoPorFormaDePagamentoDTO> porFormaDePagamento = calcularPorFormaDePagamento(pedidos);
        List<ItemRankingDTO>                   itensMaisVendidos   = calcularRankingDeItens(pedidos);
        List<PedidosPorHoraDTO>                pedidosPorHora      = calcularCurvaHoraria(pedidos);

        return new FechamentoResponseDTO(null, de, ate, resumo, porPlataforma, porFormaDePagamento, itensMaisVendidos, pedidosPorHora);
    }

    private ResumoFechamentoDTO calcularResumo(List<Pedido> pedidos) {
        BigDecimal faturamentoBruto = BigDecimal.ZERO;
        BigDecimal totalTaxas = BigDecimal.ZERO;

        for (Pedido pedido : pedidos) {
            faturamentoBruto = faturamentoBruto.add(pedido.getValor());
            totalTaxas = totalTaxas.add(calcularTaxaPlataforma(pedido)).add(calcularTaxaPagamento(pedido));
        }

        BigDecimal faturamentoLiquido = faturamentoBruto.subtract(totalTaxas);

        int totalPedidos = pedidos.size();
        BigDecimal ticketMedio;
        if (totalPedidos == 0) {
            ticketMedio = BigDecimal.ZERO;
        } else {
            double ticketMedioDouble = faturamentoBruto.doubleValue() / totalPedidos;
            ticketMedio = BigDecimal.valueOf(ticketMedioDouble).setScale(2, RoundingMode.HALF_UP);
        }

        return new ResumoFechamentoDTO(totalPedidos, faturamentoBruto, totalTaxas, faturamentoLiquido, ticketMedio);
    }

    private List<FechamentoPorPlataformaDTO> calcularPorPlataforma(List<Pedido> pedidos) {
        List<String> nomesDasPlataformas = coletarNomesUnicosDasPlataformas(pedidos);
        List<FechamentoPorPlataformaDTO> resultado = new ArrayList<>();

        for (String nomePlataforma : nomesDasPlataformas) {
            int quantidadeDePedidos = 0;
            BigDecimal bruto = BigDecimal.ZERO;
            BigDecimal taxaPlataforma = BigDecimal.ZERO;
            BigDecimal taxaPagamento = BigDecimal.ZERO;

            for (Pedido pedido : pedidos) {
                if (pedido.getPlataforma().getNome().equals(nomePlataforma)) {
                    quantidadeDePedidos++;
                    bruto = bruto.add(pedido.getValor());
                    taxaPlataforma = taxaPlataforma.add(calcularTaxaPlataforma(pedido));
                    taxaPagamento = taxaPagamento.add(calcularTaxaPagamento(pedido));
                }
            }

            BigDecimal liquido = bruto.subtract(taxaPlataforma).subtract(taxaPagamento);
            resultado.add(new FechamentoPorPlataformaDTO(nomePlataforma, quantidadeDePedidos, bruto, taxaPlataforma, taxaPagamento, liquido));
        }

        resultado.sort((plataformaA, plataformaB) -> plataformaB.bruto().compareTo(plataformaA.bruto()));
        return resultado;
    }

    private List<String> coletarNomesUnicosDasPlataformas(List<Pedido> pedidos) {
        List<String> nomes = new ArrayList<>();
        for (Pedido pedido : pedidos) {
            String nomePlataforma = pedido.getPlataforma().getNome();
            boolean jaExiste = false;
            for (String nomeJaAdicionado : nomes) {
                if (nomeJaAdicionado.equals(nomePlataforma)) {
                    jaExiste = true;
                    break;
                }
            }
            if (!jaExiste) {
                nomes.add(nomePlataforma);
            }
        }
        return nomes;
    }

    private List<FechamentoPorFormaDePagamentoDTO> calcularPorFormaDePagamento(List<Pedido> pedidos) {
        List<String> nomesDasFormasDePagamento = coletarNomesUnicosDasFormasDePagamento(pedidos);
        List<FechamentoPorFormaDePagamentoDTO> resultado = new ArrayList<>();

        for (String nomeFormaDePagamento : nomesDasFormasDePagamento) {
            int quantidadeDePedidos = 0;
            BigDecimal bruto = BigDecimal.ZERO;
            BigDecimal taxa = BigDecimal.ZERO;

            for (Pedido pedido : pedidos) {
                if (pedido.getFormaDePagamento().getNome().equals(nomeFormaDePagamento)) {
                    quantidadeDePedidos++;
                    bruto = bruto.add(pedido.getValor());
                    taxa = taxa.add(calcularTaxaPagamento(pedido));
                }
            }

            BigDecimal liquido = bruto.subtract(taxa);
            resultado.add(new FechamentoPorFormaDePagamentoDTO(nomeFormaDePagamento, quantidadeDePedidos, bruto, taxa, liquido));
        }

        resultado.sort((formaA, formaB) -> formaB.bruto().compareTo(formaA.bruto()));
        return resultado;
    }

    private List<String> coletarNomesUnicosDasFormasDePagamento(List<Pedido> pedidos) {
        List<String> nomes = new ArrayList<>();
        for (Pedido pedido : pedidos) {
            String nomeFormaDePagamento = pedido.getFormaDePagamento().getNome();
            boolean jaExiste = false;
            for (String nomeJaAdicionado : nomes) {
                if (nomeJaAdicionado.equals(nomeFormaDePagamento)) {
                    jaExiste = true;
                    break;
                }
            }
            if (!jaExiste) {
                nomes.add(nomeFormaDePagamento);
            }
        }
        return nomes;
    }

    private List<ItemRankingDTO> calcularRankingDeItens(List<Pedido> pedidos) {
        List<Long> idsDeItens = coletarIdsUnicosDeItens(pedidos);
        List<ItemRankingDTO> resultado = new ArrayList<>();

        for (Long itemId : idsDeItens) {
            String nomeDoItem = "";
            int quantidadeTotal = 0;
            BigDecimal receitaTotal = BigDecimal.ZERO;

            for (Pedido pedido : pedidos) {
                for (ItemPedido itemPedido : pedido.getItens()) {
                    if (itemPedido.getItem().getId().equals(itemId)) {
                        nomeDoItem = itemPedido.getItem().getNome();
                        quantidadeTotal += itemPedido.getQuantidade();
                        double subtotal = itemPedido.getPrecoUnitario().doubleValue() * itemPedido.getQuantidade();
                        receitaTotal = receitaTotal.add(BigDecimal.valueOf(subtotal));
                    }
                }
            }

            resultado.add(new ItemRankingDTO(itemId, nomeDoItem, quantidadeTotal, receitaTotal));
        }

        resultado.sort((itemA, itemB) -> itemB.quantidadeTotal().compareTo(itemA.quantidadeTotal()));
        return resultado;
    }

    private List<Long> coletarIdsUnicosDeItens(List<Pedido> pedidos) {
        List<Long> ids = new ArrayList<>();
        for (Pedido pedido : pedidos) {
            for (ItemPedido itemPedido : pedido.getItens()) {
                Long itemId = itemPedido.getItem().getId();
                boolean jaExiste = false;
                for (Long idJaAdicionado : ids) {
                    if (idJaAdicionado.equals(itemId)) {
                        jaExiste = true;
                        break;
                    }
                }
                if (!jaExiste) {
                    ids.add(itemId);
                }
            }
        }
        return ids;
    }

    private List<PedidosPorHoraDTO> calcularCurvaHoraria(List<Pedido> pedidos) {
        List<Integer> horasUnicas = coletarHorasUnicas(pedidos);
        List<PedidosPorHoraDTO> resultado = new ArrayList<>();

        for (int hora : horasUnicas) {
            int quantidadeDePedidos = 0;
            BigDecimal faturamentoDaHora = BigDecimal.ZERO;

            for (Pedido pedido : pedidos) {
                if (pedido.getHorarioPedido().getHour() == hora) {
                    quantidadeDePedidos++;
                    faturamentoDaHora = faturamentoDaHora.add(pedido.getValor());
                }
            }

            resultado.add(new PedidosPorHoraDTO(hora, quantidadeDePedidos, faturamentoDaHora));
        }

        resultado.sort((horaA, horaB) -> horaA.hora().compareTo(horaB.hora()));
        return resultado;
    }

    private List<Integer> coletarHorasUnicas(List<Pedido> pedidos) {
        List<Integer> horas = new ArrayList<>();
        for (Pedido pedido : pedidos) {
            int hora = pedido.getHorarioPedido().getHour();
            boolean jaExiste = false;
            for (int horaJaAdicionada : horas) {
                if (horaJaAdicionada == hora) {
                    jaExiste = true;
                    break;
                }
            }
            if (!jaExiste) {
                horas.add(hora);
            }
        }
        return horas;
    }

    private BigDecimal calcularTaxaPlataforma(Pedido pedido) {
        double valor = pedido.getValor().doubleValue();
        double percentual = pedido.getPlataforma().getTaxaPercentual().doubleValue();
        double taxa = valor * percentual / 100;
        return BigDecimal.valueOf(taxa).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularTaxaPagamento(Pedido pedido) {
        double valor = pedido.getValor().doubleValue();
        double percentual = pedido.getFormaDePagamento().getTaxaPercentual().doubleValue();
        double taxa = valor * percentual / 100;
        return BigDecimal.valueOf(taxa).setScale(2, RoundingMode.HALF_UP);
    }
}
