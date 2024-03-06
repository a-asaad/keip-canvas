function debounce<C extends (...args: Parameters<C>) => ReturnType<C>>(
  callback: C,
  wait: number
) {
  let timeoutId: ReturnType<typeof setTimeout>
  return (...args: Parameters<C>) => {
    clearTimeout(timeoutId)
    timeoutId = setTimeout(() => {
      callback(...args)
    }, wait)
  }
}

export default debounce
