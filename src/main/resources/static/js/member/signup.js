let identificationFlag = false;
const $identificationInput = $("#identification-input");
const $identificationWarning = $(".identification-error");
var idPattern = /^[a-z0-9_-]{6,20}$/;
// 아이디 정규식 이벤트 사용 및 함수
$identificationInput.on("blur", function() {
	$.ajax({
		url: "/members/checkId",
        method: 'post',
		data: {"username": $identificationInput.val()},
		success: function(result) {
			/*console.log(result);*/
			if (result.check) {
				/*	alert("성공");*/
				$identificationWarning.text("중복된 아이디입니다.");
				$identificationWarning.css("display", "block");
				$identificationInput.css("border-color", "#f66");
				identificationFlag = false;
			} else if ($identificationInput.val() < 1) {
				//$identificationWarning.text("아이디를 입력해주세요.");
				$identificationWarning.css("display", "block");
				$identificationInput.css("border-color", "#f66");
				identificationFlag = false;
				// !isPhoneNum.test(mobile.value)
			} else if (!idPattern.test($identificationInput.val())) {
				$identificationWarning.text("6~20자의 영문 소문자, 숫자와 특수기호(_),(-)만 사용 가능합니다.");
				$identificationWarning.css("display", "block");
				$identificationInput.css("border-color", "#f66");
				identificationFlag = false;
			} else {
				$identificationWarning.css("display", "none");
				$identificationInput.css("border-color", "#dde2e6");
				identificationFlag = true;
				// #dde2e6;
			}
		}
	})
});




// 이메일 변수 - 인풋
const $emailInput = $("#email-input");
const $emailWarning = $(".email-error");
// var regExp = /^[A-Za-z0-9]([-_.]?[0-9a-z]){5,20}$/;
var emailPattern = /^[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*@[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*.[a-zA-Z]{2,3}$/i;
let emailFlag = false;

$emailInput.on("blur",function(){
    $.ajax({
        url:"/members/checkEmail" ,
        method: 'post',
        data: {"memberEmail": $emailInput.val()},
        success: function(result){
            let message;
            if(result == 1){
                message = "중복된 이메일입니다.";
                $emailWarning.css('color', 'red')
                $emailWarning.css('display', 'block');
                emailFlag = false;

            } else if($emailInput.val().length < 1){
                $emailWarning.css('display', 'block');
                $emailWarning.css('color', 'red');
                message = "필수 입력 사항입니다";
                emailFlag = false;

            } else if(!emailPattern.test($emailInput.val())){
                $emailWarning.css('display', 'block');
                $emailWarning.css('color', 'red');
                message = "이메일 형식에 맞춰주세요";
                emailFlag = false;

            } else{
                message = "사용 가능한 이메일입니다.";
                $emailWarning.css('display', 'block');
                $emailWarning.css('color', '#2bb673');
                emailFlag = true;
                if(emailFlag && passwordFlag && passwordCheckFlag  && nameFlag && phoneFlag){
                    console.log("asdasd")
                    $(".signup-submit-button").css("opacity","3");
                    console.log("zz11")
                }
            }

            $emailWarning.text(message);
        }
    });
})



// 비밀번호 변수



// 비밀번호 에러 변수
const $passwordInput = $("#password-input"); //비밀번호
const $passwordWarning = $(".password-error");
let passwordFlag = false;
// 비밀번호 정규식 이벤트 사용 및 함수
$passwordInput.on("blur", function() {
    var $passwordInputValue = $passwordInput.val();
    var passwordInputValue = $passwordInput.val();
    // var num = passwordInputValue.search(/[0-9]/g);
    // var eng = passwordInputValue.search(/[a-z]/ig);
    // var spe = passwordInputValue.search(/[`~!@@#$%^&*|₩₩₩'₩";:₩/?]/gi);

    var regExp = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,20}$/;


    // $identificationInput.css("border-color", "#f66");
    // $identificationInput.css("border-color", "#dde2e6");
    if ($passwordInputValue.length < 8) {
        $passwordWarning.text("최소 8자입니다.");
        $passwordWarning.css("display", "block");
        $passwordInput.css("border-color", "#f66");
        passwordFlag = false;
    } else if ($passwordInputValue.length > 20) {
        $passwordWarning.text("최대 20자입니다.");
        $passwordWarning.css("display", "block");
        $passwordInput.css("border-color", "#f66");
        passwordFlag = false;
    } else if (!regExp.test($passwordInputValue)) {
        console.log("들어옴");
        $passwordWarning.text("영문,숫자,특수문자를 조합한 8자 이상");
        $passwordWarning.css("display", "block");
        $passwordInput.css("border-color", "#f66");
        passwordFlag = false;
    } else {
        $passwordWarning.css("display", "none");
        $passwordInput.css("border-color", "#dde2e6");
        passwordFlag = true;
        if(emailFlag && passwordFlag && passwordCheckFlag  && nameFlag && phoneFlag){
            console.log("asdasd")
            $(".signup-submit-button").css("opacity","3");
            console.log("zz11")
        }
    }
});

// 비밀번호 확인 변수
const $passwordCheckInput = $(".re-input-password-container");
// 비밀번호 확인 에러 변수
const $passwordCheckWarning = $(".recheck-password-error");
let passwordCheckFlag = false;
// 비밀번호 확인 정규식 이벤트 사용 및 함수
$passwordCheckInput.on("blur", function() {
    var $passwordInputValue = $passwordInput.val();
    var $passwordCheckInputValue = $passwordCheckInput.val();

    // $passwordCheckInput.css("border-color", "#f66");
    // $passwordCheckInput.css("border-color", "#dde2e6");
    if ($passwordCheckInputValue.length < 1) {
        $passwordCheckWarning.text("동일한 비밀번호를 입력해주세요.");
        $passwordCheckWarning.css("display", "block");
        $passwordCheckInput.css("border-color", "#f66");
        passwordCheckFlag = false;
    } else if ($passwordCheckInputValue == $passwordInputValue) {
        $passwordCheckWarning.css("display", "none");
        $passwordCheckInput.css("border-color", "#dde2e6");
        passwordCheckFlag = true;
        if(identificationFlag && passwordFlag && passwordCheckFlag  && nameFlag && phoneFlag){
            console.log("asdasd")
            $(".signup-submit-button").css("opacity","3");
            console.log("zz11")
        }
    } else {
        $passwordCheckWarning.text("동일한 비밀번호를 입력해주세요.");
        $passwordCheckWarning.css("display", "block");
        $passwordCheckInput.css("border-color", "#f66");
        passwordCheckFlag = false;
    }
});



const $nameInput = $("#name-input");
const $nameWarning = $(".name-error");
let nameFlag = false;
// 이름 정규식 이벤트 사용 및 함수
$nameInput.on("blur", function() {
    const $nameInputValue = $nameInput.val();
    const nameInputValue = $nameInput.val();
    var name = nameInputValue.search(/^[가-힣a-zA-Z]{2,20}$/);

    // $nameInput.css("border-color", "#f66");
    // $nameInput.css("border-color", "#dde2e6");
    if ($nameInputValue.length < 1) {
        $nameWarning.text("이름을 입력해주세요.");
        $nameWarning.css("display", "block");
        $nameInput.css("border-color", "#f66");
        nameFlag = false;
    } else if ($nameInputValue.length < 2) {
        $nameWarning.text("최소 2자입니다.");
        $nameWarning.css("display", "block");
        $nameInput.css("border-color", "#f66");
        nameFlag = false;
    } else if (nameInputValue.search(/\s/) != -1) {
        $nameWarning.text("다시 확인해주세요.");
        $nameWarning.css("display", "block");
        $nameInput.css("border-color", "#f66");
        nameFlag = false;
    } else if (name < 0) {
        $nameWarning.text("다시 확인해주세요.");
        $nameWarning.css("display", "block");
        $nameInput.css("border-color", "#f66");
        nameFlag = false;
    } else {
        $nameWarning.css("display", "none");
        $nameInput.css("border-color", "#dde2e6");
        nameFlag = true;
        if(identificationFlag && passwordFlag && passwordCheckFlag  && nameFlag && phoneFlag){
            console.log("asdasd")
            $(".signup-submit-button").css("opacity","3");
            console.log("zz11")
        }
    }
});


//핸드폰 변수
const $phoneInput = $('#input-phone-number');
//핸드폰 에러 변수
const $phoneWarning = $('.phone-error');
let phoneFlag = false;
// 핸드폰 정규식 이벤트 함수 사용
$phoneInput.on("blur", function() {
    var isPhoneNum = /([01]{2,})([01679]{1,})([0-9]{3,4})([0-9]{4})/;
    var $phoneInputVal = $phoneInput.val().replaceAll("-", "");
    var phoneInputVal = $phoneInput.val();
    $.ajax({
        url:"/members/checkPhone" ,
        method: 'post',
        data: {"memberPhone": $phoneInputVal},
        success: function(result){
            let message;
            if(result == 1){
                message = "중복된 번호입니다";
                $phoneWarning.css('color', 'red')
                $phoneWarning.css('display', 'block');
                phoneFlag = false;

            } else if($phoneInputVal.length < 1){
                $phoneWarning.css('display', 'block');
                $phoneWarning.css('color', 'red');
                message = "필수 입력 사항입니다";
                phoneFlag = false;

            } else if(!isPhoneNum.test($phoneInputVal)){
                $phoneWarning.css('display', 'block');
                $phoneWarning.css('color', 'red');
                message = "잘못된 형식입니다. 다시 입력해주세요.";
                phoneFlag = false;
            } else{
                message = "등록 가능한 핸드폰 번호 입니다.";
                $phoneWarning.css('display', 'block');
                $phoneWarning.css('color', '#2bb673');
                phoneFlag = true;
                if(identificationFlag && passwordFlag && passwordCheckFlag && nameFlag && phoneFlag) {
                    $(".signup-submit-button").css("opacity", "3");
                }
            }
            $phoneWarning.text(message);
        }
    });
});


/* 주소 */
function sample6_execDaumPostcode() {
    new daum.Postcode({
        oncomplete: function(data) {
            // 팝업에서 검색결과 항목을 클릭했을때 실행할 코드를 작성하는 부분.

            // 각 주소의 노출 규칙에 따라 주소를 조합한다.
            // 내려오는 변수가 값이 없는 경우엔 공백('')값을 가지므로, 이를 참고하여 분기 한다.
            var addr = ''; // 주소 변수
            var extraAddr = ''; // 참고항목 변수

            //사용자가 선택한 주소 타입에 따라 해당 주소 값을 가져온다.
            if (data.userSelectedType === 'R') { // 사용자가 도로명 주소를 선택했을 경우
                addr = data.roadAddress;
            } else { // 사용자가 지번 주소를 선택했을 경우(J)
                addr = data.jibunAddress;
            }

            // 사용자가 선택한 주소가 도로명 타입일때 참고항목을 조합한다.
            if(data.userSelectedType === 'R'){
                // 법정동명이 있을 경우 추가한다. (법정리는 제외)
                // 법정동의 경우 마지막 문자가 "동/로/가"로 끝난다.
                if(data.bname !== '' && /[동|로|가]$/g.test(data.bname)){
                    extraAddr += data.bname;
                }
                // 건물명이 있고, 공동주택일 경우 추가한다.
                if(data.buildingName !== '' && data.apartment === 'Y'){
                    extraAddr += (extraAddr !== '' ? ', ' + data.buildingName : data.buildingName);
                }
                // 표시할 참고항목이 있을 경우, 괄호까지 추가한 최종 문자열을 만든다.
                if(extraAddr !== ''){
                    extraAddr = ' (' + extraAddr + ')';
                }
                // 조합된 참고항목을 해당 필드에 넣는다.
                document.getElementById("sample6_extraAddress").value = extraAddr;

            } else {
                document.getElementById("sample6_extraAddress").value = '';
            }

            // 우편번호와 주소 정보를 해당 필드에 넣는다.
            document.getElementById('sample6_postcode').value = data.zonecode;
            document.getElementById("sample6_address").value = addr;
            // 커서를 상세주소 필드로 이동한다.
            document.getElementById("sample6_detailAddress").focus();
        }
    }).open();
}

// $(".signup-submit-button").on("change",function(
// 	if(identificationFlag && passwordFlag && passwordCheckFlag && nicknameFlag  && nameFlag && phoneFlag){
// 		console.log("asdasd")
// 		$(".signup-submit-button").removeAttr("opacity");
// 		console.log("zz11")
// 	}
// ));

$(".signup-submit-button").on("change",function(){
    if(emailFlag && passwordFlag && passwordCheckFlag  && nameFlag && phoneFlag){
        $(".signup-submit-button").css("opacity","3");
    }
});

function send() {
    if(emailFlag && passwordFlag && passwordCheckFlag  && nameFlag && phoneFlag){
        document.joinForm.submit();
    }else if(emailFlag == false){
        $emailInput.focus();
    }else if(passwordFlag == false){
        $passwordInput.focus();
    }else if(passwordCheckFlag == false){
        $passwordCheckInput.focus();
    }else if(nameFlag == false){
        $nameInput.focus();
    }else if(phoneFlag == false){
        $phoneInput.focus();
    }
}


// 이거 비밀번호 눈깔 버튼 이벤트들입니다~~
$(".Button_TextField_icon").on("click",function(){
    const type = $passwordInput.attr("type") === "password" ? "text" : "password";
    $passwordInput.attr("type", type);
    const $path = $(".Button_TextField_icon path");
    const d = $path.attr("d") === "M23.589 7.895l.411.001c10.56 0 20.16 6.504 23.34 15.78l.132.432-.144.396c-3.216 9.168-12.816 15.6-23.328 15.6A24.84 24.84 0 0 1 .684 24.492l-.168-.432.156-.42A24.804 24.804 0 0 1 24 7.896zM24 14.46a9.6 9.6 0 1 0 0 19.2 9.6 9.6 0 0 0 0-19.2zm0 4.14a5.4 5.4 0 1 1 0 10.8 5.4 5.4 0 0 1 0-10.8z" ? "M42.48 3.864l2.04 2.04-6.324 6.324-.864.852-.864.876-4.68 4.668-3.072 3.084-7.2 7.2-3.084 3.072-3.804 3.816-.912.912-.912.9-7.02 7.032-2.04-2.04 6.408-6.444a23.868 23.868 0 0 1-9.6-11.652l-.168-.432.156-.42A24.816 24.816 0 0 1 23.868 7.968c4.08.008 8.101.978 11.736 2.832l6.876-6.936zm-1.788 10.308a22.296 22.296 0 0 1 6.516 9.516l.072.444-.144.396c-3.216 9.168-12.816 15.6-23.328 15.6a25.676 25.676 0 0 1-7.872-1.2l5.532-5.532a9.06 9.06 0 0 0 2.4.312 9.6 9.6 0 0 0 9.6-9.6 9.06 9.06 0 0 0-.312-2.4zm-10.776 2.4a9.6 9.6 0 0 0-15.6 7.536l-.048-.036A9.6 9.6 0 0 0 16.332 30l3.12-2.964a5.316 5.316 0 0 1-.876-2.928 5.412 5.412 0 0 1 5.4-5.4c1.04 0 2.059.304 2.928.876z" : "M23.589 7.895l.411.001c10.56 0 20.16 6.504 23.34 15.78l.132.432-.144.396c-3.216 9.168-12.816 15.6-23.328 15.6A24.84 24.84 0 0 1 .684 24.492l-.168-.432.156-.42A24.804 24.804 0 0 1 24 7.896zM24 14.46a9.6 9.6 0 1 0 0 19.2 9.6 9.6 0 0 0 0-19.2zm0 4.14a5.4 5.4 0 1 1 0 10.8 5.4 5.4 0 0 1 0-10.8z";
    $path.attr("d", d);
})

$(".Button_TextField_icon1").on("click",function(){
    const type = $passwordCheckInput.attr("type") === "password" ? "text" : "password";
    $passwordCheckInput.attr("type", type);
    const $path = $(".Button_TextField_icon1 path");
    const d = $path.attr("d") === "M23.589 7.895l.411.001c10.56 0 20.16 6.504 23.34 15.78l.132.432-.144.396c-3.216 9.168-12.816 15.6-23.328 15.6A24.84 24.84 0 0 1 .684 24.492l-.168-.432.156-.42A24.804 24.804 0 0 1 24 7.896zM24 14.46a9.6 9.6 0 1 0 0 19.2 9.6 9.6 0 0 0 0-19.2zm0 4.14a5.4 5.4 0 1 1 0 10.8 5.4 5.4 0 0 1 0-10.8z" ? "M42.48 3.864l2.04 2.04-6.324 6.324-.864.852-.864.876-4.68 4.668-3.072 3.084-7.2 7.2-3.084 3.072-3.804 3.816-.912.912-.912.9-7.02 7.032-2.04-2.04 6.408-6.444a23.868 23.868 0 0 1-9.6-11.652l-.168-.432.156-.42A24.816 24.816 0 0 1 23.868 7.968c4.08.008 8.101.978 11.736 2.832l6.876-6.936zm-1.788 10.308a22.296 22.296 0 0 1 6.516 9.516l.072.444-.144.396c-3.216 9.168-12.816 15.6-23.328 15.6a25.676 25.676 0 0 1-7.872-1.2l5.532-5.532a9.06 9.06 0 0 0 2.4.312 9.6 9.6 0 0 0 9.6-9.6 9.06 9.06 0 0 0-.312-2.4zm-10.776 2.4a9.6 9.6 0 0 0-15.6 7.536l-.048-.036A9.6 9.6 0 0 0 16.332 30l3.12-2.964a5.316 5.316 0 0 1-.876-2.928 5.412 5.412 0 0 1 5.4-5.4c1.04 0 2.059.304 2.928.876z" : "M23.589 7.895l.411.001c10.56 0 20.16 6.504 23.34 15.78l.132.432-.144.396c-3.216 9.168-12.816 15.6-23.328 15.6A24.84 24.84 0 0 1 .684 24.492l-.168-.432.156-.42A24.804 24.804 0 0 1 24 7.896zM24 14.46a9.6 9.6 0 1 0 0 19.2 9.6 9.6 0 0 0 0-19.2zm0 4.14a5.4 5.4 0 1 1 0 10.8 5.4 5.4 0 0 1 0-10.8z";
    $path.attr("d", d);
})

