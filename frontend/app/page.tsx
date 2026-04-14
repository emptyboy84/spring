"use client";

import { useState } from "react";

export default function Home() {
   // 1. 사용자가 타이핑하는 검색창 입력값
   const [companyName, setCompanyName] = useState("");
   const [bizNumber, setBizNumber] = useState("");

   // 2. 검색이 완료되었을 때 고정해둘 데이터들
   const [officialName, setOfficialName] = useState(""); // 💡 검색 성공 시점의 회사 이름을 여기에 딱 고정합니다!
   const [result, setResult] = useState<any>(null);

   // 3. UI 상태 (로딩, 팝업)
   const [isLoading, setIsLoading] = useState(false);
   const [selectedDetail, setSelectedDetail] = useState<any>(null);

   const handleSearch = async () => {
      if (!bizNumber && !companyName) {
         alert("사업자등록번호 또는 회사이름을 입력해주세요!");
         return;
      }

      setIsLoading(true);
      setSelectedDetail(null); // 새로운 검색 시 팝업 닫기

      try {
         const url = new URL("http://localhost:8080/api/company/status");
         if (bizNumber) url.searchParams.append("bizNumber", bizNumber);
         if (companyName) url.searchParams.append("companyName", companyName);

         const response = await fetch(url.toString());

         if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || "서버 응답 에러");
         }

         const data = await response.json();

         setResult(data);

         // 💡 통신이 끝나면, 현재 검색창에 있던 '회사 이름'을 안전한 곳(searchedName)에 복사해서 묶어둡니다.
         // 💡 백엔드에서 보낸 'real_company_name'을 가져옵니다.
         // 만약 비즈노에도 이름이 없다면 사용자가 입력했던 이름을 백업으로 사용합니다.
         if (data.data && data.data.length > 0) {
            const foundName = data.data[0].real_company_name;
            setOfficialName(foundName !== "이름미상" ? foundName : companyName || "이름미상");
         }


      } catch (error) {
         console.error("에러:", error);
         alert("서버 통신 중 문제가 발생했습니다.");
      } finally {
         setIsLoading(false);
      }
   };

   const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
      if (e.key === 'Enter') {
         handleSearch();
      }
   };

   const isValidCompany = result && result.data && result.data.length > 0 && result.data[0].b_stt !== "";

   return (
      <main className="p-10 font-sans min-h-screen bg-gray-50 text-black relative">
         <div className="max-w-4xl mx-auto">
            <h1 className="text-3xl font-bold mb-8 text-gray-800">기업 정보 검색 🏢</h1>

            {/* 검색 폼 */}
            <div className="flex gap-4 mb-8 bg-white p-6 rounded-lg shadow-sm border border-gray-200">
               <input
                  type="text"
                  placeholder="회사 이름(선택사항)"
                  className="border border-gray-300 p-3 rounded w-1/3 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  value={companyName}
                  onChange={(e) => setCompanyName(e.target.value)}//event.target.value는 사용자가 입력한 값
                  onKeyDown={handleKeyDown}
               />
               <input
                  type="text"
                  placeholder="사업자등록번호 (- 없이)"
                  className="border border-gray-300 p-3 rounded w-1/3 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  value={bizNumber}
                  onChange={(e) => setBizNumber(e.target.value)}
                  onKeyDown={handleKeyDown}
               />
               <button
                  onClick={handleSearch}
                  disabled={isLoading}
                  className={`text-white px-8 py-3 rounded transition font-semibold w-1/3 flex justify-center items-center ${isLoading ? "bg-gray-400 cursor-not-allowed" : "bg-blue-600 hover:bg-blue-700"
                     }`}
               >
                  {isLoading ? "조회 중... ⏳" : "조회하기"}
               </button>
            </div>

            {/* 검색 결과 영역 */}
            {isValidCompany ? (
               <div className="bg-white rounded-lg shadow overflow-hidden border border-gray-200 animate-fade-in">
                  <table className="w-full text-left border-collapse">
                     <thead>
                        <tr className="bg-gray-100 text-gray-700">
                           <th className="p-4 border-b font-semibold">확인된 회사명</th>
                           <th className="p-4 border-b font-semibold">사업자번호</th>
                           <th className="p-4 border-b font-semibold">영업 상태</th>
                           <th className="p-4 border-b font-semibold text-center">상세보기</th>
                        </tr>
                     </thead>
                     <tbody>
                        <tr
                           className="hover:bg-blue-50 transition cursor-pointer group"
                           onClick={() => setSelectedDetail(result.data[0])}
                        >
                           {/* 💡 검색창(companyName)이 아니라, 고정된 이름(searchedName)을 보여줍니다 */}
                           <td className="p-4 border-b font-medium text-gray-800">{officialName}
                              {/* 예를들어 사용자가 '삼성전자'를 입력하고 조회했을 때, 백엔드가 '삼성전자 주식회사'로 찾아주면, 
                              두 이름이 다르므로 괄호 안에 '자동확인'이라고 표시해주는 로직입니다. */}
                              {officialName === companyName ? "" : <span className="ml-2 text-gray-500">(자동확인)</span>}
                           </td>
                           <td className="p-4 border-b">{result.data[0].formatted_bno || result.data[0].b_no}</td>
                           <td className="p-4 border-b">
                              <span className={`px-3 py-1 rounded-full text-sm font-bold ${result.data[0].b_stt === '계속사업자' //계속사업자이면 파란색, 아니면 빨간색
                                 ? 'bg-blue-100 text-blue-700'
                                 : 'bg-red-100 text-red-700'
                                 }`}>
                                 {result.data[0].b_stt}
                              </span>
                           </td>
                           <td className="p-4 border-b text-center">
                              <span className="text-blue-500 font-semibold group-hover:underline">보기 🔍</span>
                           </td>
                        </tr>
                     </tbody>
                  </table>
               </div>
            ) : result && (
               <div className="p-4 bg-red-50 text-red-600 rounded-lg border border-red-200 text-center font-medium animate-fade-in">
                  ❌ 국세청에 등록되지 않은 사업자등록번호이거나, 잘못된 번호입니다.
               </div>
            )}
         </div>

         {/* 상세 정보 팝업(모달) */}
         {selectedDetail && (
            <div className="fixed inset-0 bg-black bg-opacity-50 flex justify-center items-center z-50 p-4">
               <div className="bg-white p-8 rounded-lg shadow-2xl w-full max-w-md animate-pop-in">
                  <h2 className="text-2xl font-bold border-b pb-4 mb-4 text-gray-800">
                     기업상세정보
                  </h2>


                  <div className="space-y-3 text-sm text-gray-700">
                     <p className="flex justify-between border-b border-gray-50 pb-2">
                        <span className="font-semibold text-gray-500">공식 상호명</span>
                        {/* 💡 팝업에서도 고정된 이름(searchedName)을 보여줍니다 */}
                        <span className="font-bold text-lg bg-gray-100 px-2 py-1 rounded">
                           {officialName}
                        </span>
                     </p>

                     <p className="flex justify-between">
                        <span className="font-semibold text-gray-500">사업자번호</span>
                        <span className="font-bold">{selectedDetail.formatted_bno || selectedDetail.b_no}</span>
                     </p>

                     <p className="flex justify-between">
                        <span className="font-semibold text-gray-500">영업 상태</span>
                        <span className={`font-bold ${selectedDetail.b_stt === '계속사업자' ? 'text-blue-600' : 'text-red-600'}`}>
                           {selectedDetail.b_stt}
                        </span>
                     </p>
                     <p className="flex justify-between">
                        <span className="font-semibold text-gray-500">과세 유형</span>
                        <span className="font-bold">{selectedDetail.tax_type}</span>
                     </p>

                     {selectedDetail.end_dt && (
                        <p className="flex justify-between">
                           <span className="font-semibold text-gray-500">폐업 일자</span>
                           <span className="font-bold text-red-600">{selectedDetail.end_dt}</span>
                        </p>
                     )}

                     {/* 추가된 확장 데이터 표시 영역 */}
                     <div className="border-t border-gray-200 pt-4 mt-4 space-y-3">
                        <h3 className="text-gray-400 font-semibold text-xs mb-2">상세 확인 정보</h3>
                        
                        {selectedDetail.industry && (
                           <p className="flex justify-between items-start gap-4">
                              <span className="font-semibold text-gray-500 whitespace-nowrap">업종(업태)</span>
                              <span className="font-bold text-gray-800 text-right">{selectedDetail.industry}</span>
                           </p>
                        )}
                        {selectedDetail.address && (
                           <p className="flex justify-between items-start gap-4">
                              <span className="font-semibold text-gray-500 whitespace-nowrap">상세 주소</span>
                              <span className="font-bold text-gray-800 text-right">{selectedDetail.address}</span>
                           </p>
                        )}
                        {selectedDetail.phone && (
                           <p className="flex justify-between items-center">
                              <span className="font-semibold text-gray-500">연락처</span>
                              <span className="font-bold text-blue-600">{selectedDetail.phone}</span>
                           </p>
                        )}
                        {selectedDetail.fax && (
                           <p className="flex justify-between items-center">
                              <span className="font-semibold text-gray-500">팩스 번호</span>
                              <span className="font-bold text-gray-700">{selectedDetail.fax}</span>
                           </p>
                        )}
                        {selectedDetail.email && (
                           <p className="flex justify-between items-center">
                              <span className="font-semibold text-gray-500">메일 주소</span>
                              <span className="font-bold text-gray-800">{selectedDetail.email}</span>
                           </p>
                        )}
                        {selectedDetail.homepage && (
                           <p className="flex justify-between items-center">
                              <span className="font-semibold text-gray-500">홈페이지</span>
                              <a href={selectedDetail.homepage} target="_blank" rel="noreferrer" className="font-bold text-blue-500 underline truncate max-w-[200px] hover:text-blue-700">
                                 {selectedDetail.homepage}
                              </a>
                           </p>
                        )}
                        {selectedDetail.revenue && (
                           <p className="flex justify-between items-center">
                              <span className="font-semibold text-gray-500">최근 1년 매출액</span>
                              <span className="font-bold text-green-700">
                                 {Number(selectedDetail.revenue).toLocaleString()} 원
                              </span>
                           </p>
                        )}
                        {selectedDetail.employeeCount && (
                           <p className="flex justify-between items-center">
                              <span className="font-semibold text-gray-500">직원수(국민연금)</span>
                              <span className="font-bold text-purple-700">
                                 {Number(selectedDetail.employeeCount).toLocaleString()} 명
                              </span>
                           </p>
                        )}
                     </div>
                  </div>

                  <button
                     onClick={() => setSelectedDetail(null)}
                     className="mt-8 w-full bg-gray-800 text-white py-2 rounded hover:bg-gray-900 transition font-semibold"
                  >
                     닫기
                  </button>
               </div>
            </div>
         )}
      </main>
   );
}