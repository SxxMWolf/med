/**
 * 날짜 문자열을 한국어 형식으로 포맷팅
 * @param dateString ISO 8601 형식의 날짜 문자열
 * @returns 포맷팅된 날짜 문자열
 */
export const formatDate = (dateString: string): string => {
  const date = new Date(dateString);
  return date.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

