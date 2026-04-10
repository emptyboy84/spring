"use client";

import { useEffect, useState } from "react";


export default function Home() {
   const [companyName, setCompanyName] = useState("");
   const [bizNumber, setBizNumber] = useState("");
   const [result, setResult] = useState<any>(null);
   const [isMounted, setIsMounted] = useState(false);//컴포넌트가 마운트 되었는지 확인하는 변수

   // 💡 UX 추가 1: 로딩 상태를 관리하는 변수
   const [isLoading, setIsLoading] = useState(false);
   let [isValidCompany, setIsValidCompany] = useState(false);
   // 💡 상세 정보 팝업(모달)에 띄울 데이터를 저장하는 상태
   const [selectedDetail, setSelectedDetail] = useState<any>(null);

   useEffect(() => {
      setIsMounted(true);// "나 브라우저에 안전히 도착했어!" 도장 쾅
   }, []);


   const handleSearch = async () => {
      if (!bizNumber) {
         alert("사업자등록번호를 입력해주세요!");
         return;
      } else if (!isMounted) {
         return <div>로딩중...</div>//서버에서 데이터를 받아올때까지 화면에 로딩중이라고 표시
      }

      // 💡 UX 추가 2: 통신시 로딩 상태를 true로 변경
      setIsLoading(true);//로딩 시작

      // 새로운 검색 시 기존 팝업 닫기
      setSelectedDetail(null);


      try {
         const response = await fetch(`http://localhost:8080/api/company/status?bizNumber=${bizNumber}`);
         const data = await response.json();// 비동기 통신후 응답을 json으로 변환
         setResult(data);//결과를 상태에 저장
      } catch (error) {
         console.error("에러:", error);
         alert("서버 통신 중 문제가 발생했습니다.");
      } finally {
         // 💡 UX 추가 3: 통신이 끝나면 로딩 상태를 false로 변경 (성공하든 실패하든 무조건 실행)
         setIsLoading(false); //로딩 종료
      }
   };
   // 💡 UX 추가 4: 엔터키(Enter)를 감지하는 함수
   const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {//키보드 이벤트 발생시
      if (e.key === "Enter") {//엔터키를 누르면
         handleSearch(); //검색 함수 호출
      }
   };

   isValidCompany = result && result.data && result.data.length > 0 && result.data[0].b_stt !== "";



   return (
      <main className="p-10 font-sans min-h-screen bg-gray-50 text-black">
         <div className="max-w-4xl mx-auto">
            <h1 className="text-3xl font-bold mb-8 text-gray-800">기업 정보 검색 🏢</h1>

            {/* 검색 폼 */}
            <div className="flex gap-4 mb-8 bg-white p-6 rounded-lg shadow-sm border border-gray-200">
               <input
                  type="text"
                  placeholder="회사 이름 "
                  className="border border-gray-300 p-3 rounded w-1/3 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  value={companyName}
                  onChange={(e) => setCompanyName(e.target.value)}
                  onKeyDown={handleKeyDown} //엔터키 연결
               />
               <input
                  type="text"
                  placeholder="사업자등록번호 (- 없이)"
                  className="border border-gray-300 p-3 rounded w-1/3 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  value={bizNumber}
                  onChange={(e) => setBizNumber(e.target.value)}
                  onKeyDown={handleKeyDown} //엔터키 연결
               />
               <button
                  onClick={handleSearch}
                  disabled={isLoading}  //로딩중일때 버튼 비활성화
                  className={`text-white px-8 py-3 rounded transition font-semibold w-1/3 flex justify-center 
                  items-center ${isLoading ? "bg-gray-400 cursor-not-allowed" : "bg-blue-600 hover:bg-blue-700"}`}
               >
                  {isLoading ? "조회중... ⏳" : "조회하기"} {/*로딩중일때 버튼 텍스트 변경*/}
               </button>
            </div>
            {/* 💡 UX 추가 5: 검색 결과가 없을 때 보여줄 메시지 */}
            {result && result.data && result.data.length === 0 && (
               <div className="text-center py-12 bg-white rounded-lg shadow border border-gray-200">
                  <p className="text-gray-500 text-lg">검색 결과가 없습니다. 🔍</p>
               </div>
            )}
            {/* 💡 팝   업(모달) 창: selectedDetail 데이터가 있을 때만 화면에 그려집니다. */}
            {selectedDetail && (
               <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                  <div className="bg-white p-8 rounded-lg shadow-xl max-w-2xl w-full">
                     <h2 className="text-2xl font-bold mb-6">상세 정보</h2>
                     <div className="space-y-4">
                        <div className="flex justify-between">
                           <span className="text-gray-500">회사명</span>
                           <span className="font-semibold">{selectedDetail.companyName}</span>
                        </div>
                        <div className="flex justify-between">
                           <span className="text-gray-500">사업자번호</span>
                           <span className="font-semibold">{selectedDetail.bizNumber}</span>
                        </div>
                        <div className="flex justify-between">
                           <span className="text-gray-500">영업상태</span>
                           <span className="font-semibold">{selectedDetail.b_stt}</span>
                        </div>
                        <div className="flex justify-between">
                           <span className="text-gray-500">과세유형</span>
                           <span className="font-semibold">{selectedDetail.tax_type}</span>
                        </div>
                     </div>
                     <button
                        onClick={() => setSelectedDetail(null)}
                        className="mt-6 w-full bg-gray-600 text-white py-2 rounded hover:bg-gray-700"
                     >
                        닫기
                     </button>
                  </div>
               </div>
            )}
            {/* 검색 결과 표 (Table) */}
            {/* 데이터가 존재하는지 확인하는 방어막 (삼항 연산자) */}
            {/* result가 있고, result.data가 있고, 데이터 개수가 0보다 클 때만 아랫부분을 화면에 그려라! */}

            {/* 옳은 회사일 경우: 정상적으로 표를 그려줍니다. */}
            {result && result.data && result.data.length > 0 && isValidCompany ? (
               <div className="bg-white rounded-lg shadow overflow-hidden border border-gray-200">
                  {/* 1. 표의 머리글(제목 줄) 영역 */}
                  <table className="w-full text-left border-collapse">
                     <thead>
                        <tr className="bg-gray-100 text-gray-700">
                           <th className="p-4 border-b font-semibold">회사명</th>
                           <th className="p-4 border-b font-semibold">사업자번호</th>
                           <th className="p-4 border-b font-semibold">영업상태</th>
                           <th className="p-4 border-b font-semibold">과세유형</th>
                        </tr>
                     </thead>
                     <tbody>
                        {/* 2. 표의 본문(실제 데이터) 영역 */}
                        <tr className="hover:bg-gray-50 transition cursor-pointer">
                           <td className="p-4 border-b">{companyName || "-"}</td>
                           <td className="p-4 border-b">{result.data[0].b_no}</td>
                           <td className="p-4 border-b">
                              {/* 상태가 '계속사업자'이면 파란색, 아니면 빨간색으로 표시 */}
                              <span className={`px-3 py-1 rounded-full text-sm font-bold ${result.data[0].b_stt === '계속사업자'
                                 ? 'bg-blue-100 text-blue-700'
                                 : 'bg-red-100 text-red-700'
                                 }`}>
                                 {/* API가 보내준 데이터(result) 안의 첫 번째 배열(data[0])에서 사업자번호(b_no)를 쏙 빼옵니다. */}
                                 {result.data[0].b_stt}
                              </span>
                           </td>
                           <td className="p-4 border-b text-gray-600">{result.data[0].tax_type}</td>
                        </tr>
                     </tbody>
                  </table>
               </div>
            ) : result && ( // result가 있고, result.data가 없고, 데이터 개수가 0보다 작을 때만 아랫부분을 화면에 그려라!
               /* 2. 틀린 번호(가짜 번호)일 경우: 표 대신 에러 메시지를 보여줍니다. */
               <div className="p-4 bg-red-50 text-red-600 rounded-lg border border-red-200 text-center font-medium animate-fade-in">
                  조회된 데이터가 없거나 잘못된 사업자번호입니다.
               </div>
            )}
         </div>
      </main>
   );
}