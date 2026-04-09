"use client"//프론트엔드에서 백엔드로 통신
import { useState } from "react";

export default function Home() {
   // 사용자가 입력한 값을 저장하는 공간 (상태)
   const [companyname, setCompanyName] = useState("");//회사명
   const [biznumber, setBiznumber] = useState("");//사업자번호
   // 백엔드에서 받아온 결과를 저장하는 공간
   const [result, setResult] = useState<any>(null); //null은 아직 결과가 없다는 뜻

   // 검색버튼 클릭시 실행될 함수
   const handleSearch = async () => {
      if (!biznumber) {
         alert("없는회사입니다.");
         return;
      }
      try {
         const response = await fetch(`http://localhost:8080/api/company/${biznumber}`);
         const data = await response.json();
         setResult(data);
      } catch (error) {
         console.error("Error fetching company data:", error);
      }
   }



}

