export function ehCaminhoInterno(valor: unknown): valor is string {
  if (typeof valor !== 'string') return false
  if (!valor.startsWith('/')) return false
  return !valor.startsWith('//') && !valor.includes('\\')
}
