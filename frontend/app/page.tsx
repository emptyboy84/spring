"use client";

import { useState } from "react";

interface SearchItem {
   company: string;
   bno: string;
   ceo: string;
   address: string;
   biz_type: string;
   tel: string;
   bstt: string;
   employeeCount?: string;
}

export default function Home() {
   // 1. 사용자가 타이핑하는 검색창 입력값
   const [companyName, setCompanyName] = useState("");
   const [bizNumber, setBizNumber] = useState("");

   // 2. 검색 결과 목록 (회사명 검색 시)
   const [searchResults, setSearchResults] = useState<SearchItem[]>([]);
   const [searchQuery, setSearchQuery] = useState("");

   // 3. 상세 조회 결과 (사업자번호로 조회 시)
   const [officialName, setOfficialName] = useState("");
   const [result, setResult] = useState<any>(null);

   // 4. UI 상태 (로딩, 팝업)
   const [isLoading, setIsLoading] = useState(false);
   const [selectedDetail, setSelectedDetail] = useState<any>(null);

   // 🔍 회사명으로 검색 → 목록 반환
   const handleSearch = async () => {
      if (!bizNumber && !companyName) {
         alert("사업자등록번호 또는 회사이름을 입력해주세요!");
         return;
      }

      setIsLoading(true);
      setSelectedDetail(null);
      setResult(null);
      setSearchResults([]);

      // 사업자번호가 있으면 바로 상세 조회
      if (bizNumber) {
         await fetchDetail(bizNumber, "");
         setIsLoading(false);
         return;
      }

      // 회사명만 있으면 검색 목록 조회
      try {
         //const url = `http://localhost:8080/api/company/search?q=${encodeURIComponent(companyName)}`;
         const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";
         const response = await fetch(`${API_URL}/api/company/status?bizNumber=${bizNumber}`);

         //const response = await fetch(url);
         const data = await response.json();

         setSearchQuery(companyName);
         if (data.items && data.items.length > 0) {
            setSearchResults(data.items);
         } else {
            setSearchResults([]);
            alert("검색 결과가 없습니다.");
         }
      } catch (error) {
         console.error("검색 에러:", error);
         alert("서버 통신 중 문제가 발생했습니다.");
      } finally {
         setIsLoading(false);
      }
   };

   // 📋 목록에서 선택 → 상세 조회
   const fetchDetail = async (bno: string, name: string) => {
      setIsLoading(true);
      try {
         const url = new URL("http://localhost:8080/api/company/status");
         url.searchParams.append("bizNumber", bno);

         const response = await fetch(url.toString());
         if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || "서버 응답 에러");
         }

         const data = await response.json();
         setResult(data);

         if (data.data && data.data.length > 0) {
            const foundName = data.data[0].real_company_name;
            setOfficialName(foundName !== "이름미상" ? foundName : name || "이름미상");
            setSelectedDetail(data.data[0]);
         }
      } catch (error) {
         console.error("상세 조회 에러:", error);
         alert("상세 정보를 가져오는 중 문제가 발생했습니다.");
      } finally {
         setIsLoading(false);
      }
   };

   const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
      if (e.key === 'Enter') {
         handleSearch();
      }
   };

   // 사업자번호 직접 조회 시 결과 표시 체크
   const isValidCompany = result && result.data && result.data.length > 0 && result.data[0].b_stt !== "";

   return (
      <main className="p-10 font-sans min-h-screen bg-gray-50 text-black relative">
         <div className="max-w-5xl mx-auto">
            <h1 className="text-3xl font-bold mb-8 text-gray-800">기업 정보 검색 🏢</h1>

            {/* 검색 폼 */}
            <div className="flex gap-4 mb-8 bg-white p-6 rounded-lg shadow-sm border border-gray-200">
               <input
                  type="text"
                  placeholder="회사 이름 (예: LG전자)"
                  className="border border-gray-300 p-3 rounded w-1/3 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  value={companyName}
                  onChange={(e) => setCompanyName(e.target.value)}
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

            {/* ============================================ */}
            {/* 회사명 검색 결과 목록 (여러 개) */}
            {/* ============================================ */}
            {searchResults.length > 0 && (
               <div className="bg-white rounded-lg shadow overflow-hidden border border-gray-200 animate-fade-in mb-8">
                  <div className="bg-blue-600 text-white px-6 py-3 flex justify-between items-center">
                     <span className="font-bold text-lg">
                        &quot;{searchQuery}&quot; 검색 결과 ({searchResults.length}건)
                     </span>
                     <span className="text-blue-100 text-sm">클릭하면 상세 정보를 조회합니다</span>
                  </div>
                  <table className="w-full text-left border-collapse">
                     <thead>
                        <tr className="bg-gray-100 text-gray-700">
                           <th className="p-3 border-b font-semibold text-sm">#</th>
                           <th className="p-3 border-b font-semibold text-sm">회사명</th>
                           <th className="p-3 border-b font-semibold text-sm">사업자번호</th>
                           <th className="p-3 border-b font-semibold text-sm">대표자</th>
                           <th className="p-3 border-b font-semibold text-sm">주소</th>
                           <th className="p-3 border-b font-semibold text-sm">업종</th>
                           <th className="p-3 border-b font-semibold text-sm">상태</th>
                           <th className="p-3 border-b font-semibold text-sm text-center">직원수</th>
                        </tr>
                     </thead>
                     <tbody>
                        {searchResults.map((item, idx) => (
                           <tr
                              key={idx}
                              className="hover:bg-blue-50 transition cursor-pointer group border-b border-gray-100"
                              onClick={() => {
                                 const bno = item.bno.replace(/-/g, "").trim();
                                 if (bno) fetchDetail(bno, item.company);
                              }}
                           >
                              <td className="p-3 text-gray-400 text-sm">{idx + 1}</td>
                              <td className="p-3 font-medium text-gray-800 group-hover:text-blue-600">
                                 {item.company}
                              </td>
                              <td className="p-3 text-sm text-gray-600 font-mono">{item.bno}</td>
                              <td className="p-3 text-sm text-gray-600">{item.ceo || "-"}</td>
                              <td className="p-3 text-sm text-gray-500 max-w-[200px] truncate" title={item.address}>
                                 {item.address || "-"}
                              </td>
                              <td className="p-3 text-sm text-gray-500 max-w-[150px] truncate" title={item.biz_type}>
                                 {item.biz_type || "-"}
                              </td>
                              <td className="p-3">
                                 {item.bstt && (
                                    <span className={`px-2 py-0.5 rounded-full text-xs font-bold ${item.bstt === '계속사업자'
                                          ? 'bg-blue-100 text-blue-700'
                                          : 'bg-red-100 text-red-700'
                                       }`}>
                                       {item.bstt}
                                    </span>
                                 )}
                              </td>
                              <td className="p-3 text-center text-sm font-bold text-purple-600">
                                 {item.employeeCount ? `${Number(item.employeeCount).toLocaleString()}명` : "-"}
                              </td>
                           </tr>
                        ))}
                     </tbody>
                  </table>
               </div>
            )}

            {/* ============================================ */}
            {/* 사업자번호 직접 조회 결과 (1건) */}
            {/* ============================================ */}
            {bizNumber && isValidCompany && (
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
                           <td className="p-4 border-b font-medium text-gray-800">{officialName}
                              {officialName !== companyName && <span className="ml-2 text-gray-500">(자동확인)</span>}
                           </td>
                           <td className="p-4 border-b">{result.data[0].formatted_bno || result.data[0].b_no}</td>
                           <td className="p-4 border-b">
                              <span className={`px-3 py-1 rounded-full text-sm font-bold ${result.data[0].b_stt === '계속사업자'
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
            )}

            {bizNumber && result && !isValidCompany && (
               <div className="p-4 bg-red-50 text-red-600 rounded-lg border border-red-200 text-center font-medium animate-fade-in">
                  ❌ 국세청에 등록되지 않은 사업자등록번호이거나, 잘못된 번호입니다.
               </div>
            )}
         </div>

         {/* 상세 정보 팝업(모달) */}
         {selectedDetail && (
            <div className="fixed inset-0 bg-black bg-opacity-50 flex justify-center items-center z-50 p-4">
               <div className="bg-white p-8 rounded-lg shadow-2xl w-full max-w-md animate-pop-in max-h-[90vh] overflow-y-auto">
                  <h2 className="text-2xl font-bold border-b pb-4 mb-4 text-gray-800">
                     기업상세정보
                  </h2>

                  <div className="space-y-3 text-sm text-gray-700">
                     <p className="flex justify-between border-b border-gray-50 pb-2">
                        <span className="font-semibold text-gray-500">공식 상호명</span>
                        <span className="font-bold text-lg bg-gray-100 px-2 py-1 rounded">
                           {officialName}
                        </span>
                     </p>

                     <p className="flex justify-between">
                        <span className="font-semibold text-gray-500">사업자번호</span>
                        <span className="font-bold">{selectedDetail.formatted_bno || selectedDetail.b_no}</span>
                     </p>

                     <p className="flex justify-between">
                        <span className="font-semibold text-gray-500">대표자</span>
                        <span className={`font-bold ${selectedDetail.ceo ? 'text-gray-800' : 'text-gray-300 italic'}`}>
                           {selectedDetail.ceo || "정보 없음"}
                        </span>
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

                        <p className="flex justify-between items-start gap-4">
                           <span className="font-semibold text-gray-500 whitespace-nowrap">업종(업태)</span>
                           <span className={`font-bold text-right ${selectedDetail.industry ? 'text-gray-800' : 'text-gray-300 italic'}`}>
                              {selectedDetail.industry || "정보 없음"}
                           </span>
                        </p>
                        <p className="flex justify-between items-start gap-4">
                           <span className="font-semibold text-gray-500 whitespace-nowrap">상세 주소</span>
                           <span className={`font-bold text-right ${selectedDetail.address ? 'text-gray-800' : 'text-gray-300 italic'}`}>
                              {selectedDetail.address || "정보 없음"}
                           </span>
                        </p>
                        <p className="flex justify-between items-center">
                           <span className="font-semibold text-gray-500">연락처</span>
                           <span className={`font-bold ${selectedDetail.phone ? 'text-blue-600' : 'text-gray-300 italic'}`}>
                              {selectedDetail.phone || "정보 없음"}
                           </span>
                        </p>
                        <p className="flex justify-between items-center">
                           <span className="font-semibold text-gray-500">팩스 번호</span>
                           <span className={`font-bold ${selectedDetail.fax ? 'text-gray-700' : 'text-gray-300 italic'}`}>
                              {selectedDetail.fax || "정보 없음"}
                           </span>
                        </p>
                        <p className="flex justify-between items-center">
                           <span className="font-semibold text-gray-500">메일 주소</span>
                           <span className={`font-bold ${selectedDetail.email ? 'text-gray-800' : 'text-gray-300 italic'}`}>
                              {selectedDetail.email || "정보 없음"}
                           </span>
                        </p>
                        <p className="flex justify-between items-center">
                           <span className="font-semibold text-gray-500">홈페이지</span>
                           {selectedDetail.homepage ? (
                              <a href={selectedDetail.homepage} target="_blank" rel="noreferrer" className="font-bold text-blue-500 underline truncate max-w-[200px] hover:text-blue-700">
                                 {selectedDetail.homepage}
                              </a>
                           ) : (
                              <span className="font-bold text-gray-300 italic">정보 없음</span>
                           )}
                        </p>
                        <p className="flex justify-between items-center">
                           <span className="font-semibold text-gray-500">
                              매출액{selectedDetail.revenueYear ? ` (${selectedDetail.revenueYear}년)` : ""}
                           </span>
                           <span className={`font-bold ${selectedDetail.revenue ? 'text-green-700' : 'text-gray-300 italic'}`}>
                              {selectedDetail.revenue ? `${Number(selectedDetail.revenue).toLocaleString()} 원` : "정보 없음"}
                           </span>
                        </p>
                        <p className="flex justify-between items-center">
                           <span className="font-semibold text-gray-500">
                              직원수{selectedDetail.employeeYear ? ` (${selectedDetail.employeeYear}년)` : ""}
                           </span>
                           <span className={`font-bold ${selectedDetail.employeeCount ? 'text-purple-700' : 'text-gray-300 italic'}`}>
                              {selectedDetail.employeeCount ? `${Number(selectedDetail.employeeCount).toLocaleString()} 명` : "정보 없음"}
                           </span>
                        </p>
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

         {/* 로딩 오버레이 */}
         {isLoading && (
            <div className="fixed inset-0 bg-black bg-opacity-30 flex justify-center items-center z-40">
               <div className="bg-white px-8 py-4 rounded-lg shadow-lg text-gray-700 font-semibold text-lg">
                  ⏳ 기업 정보 조회 중...
               </div>
            </div>
         )}
      </main>
   );
}