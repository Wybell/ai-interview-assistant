export function formatDateTime(value: string): string {
  const normalized = value.includes('T') ? value : value.replace(' ', 'T');
  const date = new Date(normalized);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

export function getScoreTone(score: number): 'success' | 'warning' | 'danger' {
  if (score >= 8) {
    return 'success';
  }
  if (score >= 6) {
    return 'warning';
  }
  return 'danger';
}

export function getScoreLabel(score: number): string {
  if (score >= 8) {
    return '掌握良好';
  }
  if (score >= 6) {
    return '继续巩固';
  }
  return '需要复盘';
}
