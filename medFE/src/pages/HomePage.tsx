import { Link } from 'react-router-dom';

export default function HomePage() {
  const features = [
    {
      title: '알러지 관리',
      description: '복용하면 안 되는 성분을 등록하고 관리하세요',
      path: '/allergies',
      color: 'bg-red-50 border-red-200',
      icon: '⚠️',
    },
    {
      title: '증상 분석',
      description: '현재 증상을 입력하면 안전한 약을 추천받으세요',
      path: '/symptom',
      color: 'bg-blue-50 border-blue-200',
      icon: '🔍',
    },
    {
      title: '부작용 분석',
      description: '복용했던 약들의 공통 성분과 위험 패턴을 분석하세요',
      path: '/side-effect',
      color: 'bg-yellow-50 border-yellow-200',
      icon: '💊',
    },
    {
      title: '성분표 분석',
      description: '약 성분표 사진을 업로드하여 안전성을 확인하세요',
      path: '/ocr',
      color: 'bg-green-50 border-green-200',
      icon: '📷',
    },
  ];

  return (
    <div className="px-4 py-8">
      <div className="text-center mb-12">
        <h1 className="text-4xl font-bold text-gray-900 mb-4">
          개인 맞춤형 복약 안전성 확인
        </h1>
        <p className="text-xl text-gray-600">
          알러지 정보와 복용 경험을 바탕으로 안전한 약물을 선택하세요
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 max-w-5xl mx-auto">
        {features.map((feature) => (
          <Link
            key={feature.path}
            to={feature.path}
            className={`${feature.color} border-2 rounded-lg p-6 hover:shadow-lg transition-shadow`}
          >
            <div className="text-4xl mb-4">{feature.icon}</div>
            <h2 className="text-2xl font-semibold text-gray-900 mb-2">
              {feature.title}
            </h2>
            <p className="text-gray-700">{feature.description}</p>
          </Link>
        ))}
      </div>
    </div>
  );
}

