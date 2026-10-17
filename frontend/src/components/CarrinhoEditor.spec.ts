import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import CarrinhoEditor from '@/components/CarrinhoEditor.vue'
import type { ItemResponse } from '@/api/types'
import type { LinhaCarrinho } from '@/types'

const ITENS: ItemResponse[] = [
  {
    id: 7,
    nome: 'Batata frita',
    categoriaId: 1,
    categoriaNome: 'Porções',
    preco: 18.5,
    status: 'ATIVO',
    descricao: null,
  },
]

async function adicionarComQuantidade(quantidade: string, carrinho: LinhaCarrinho[] = []) {
  const editor = mount(CarrinhoEditor, { props: { modelValue: carrinho, itens: ITENS } })
  await editor.get('select').setValue('7')
  await editor.get('input[type="number"]').setValue(quantidade)
  await editor.get('button').trigger('click')
  return editor
}

describe('CarrinhoEditor', () => {
  it('adiciona o item quando a quantidade é um inteiro a partir de 1', async () => {
    const editor = await adicionarComQuantidade('2')

    expect(editor.emitted('update:modelValue')).toEqual([
      [[{ itemId: 7, nome: 'Batata frita', preco: 18.5, quantidade: 2 }]],
    ])
    expect(editor.text()).not.toContain('Quantidade inválida.')
  })

  it('soma à linha existente quando o item já está no carrinho', async () => {
    const editor = await adicionarComQuantidade('3', [
      { itemId: 7, nome: 'Batata frita', preco: 18.5, quantidade: 1 },
    ])

    expect(editor.emitted('update:modelValue')).toEqual([
      [[{ itemId: 7, nome: 'Batata frita', preco: 18.5, quantidade: 4 }]],
    ])
  })

  it.each(['1.5', '0.5', '0', '-2', ''])('recusa a quantidade "%s" sem alterar o carrinho', async (quantidade) => {
    const editor = await adicionarComQuantidade(quantidade)

    expect(editor.text()).toContain('Quantidade inválida.')
    expect(editor.emitted('update:modelValue')).toBeUndefined()
  })
})
