/* login */
$(document).ready(function() {
    var memberLogin = $('span.member-login');
    var enterpriseLogin = $('span.enterprise-login');

    memberLogin.on('click', function() {
        memberLogin.css('border-bottom', '2px solid');
        memberLogin.css('color', '');
        enterpriseLogin.css('border-bottom', '');
        enterpriseLogin.css('color', '#9fa1a7');
    });

    enterpriseLogin.on('click', function() {
        enterpriseLogin.css('border-bottom', '2px solid');
        enterpriseLogin.css('color', 'black');
        memberLogin.css('border-bottom', '0');
        memberLogin.css('color', '#9fa1a7');
    });
});



const $id = $("input#id");
const $password = $("input#password");


function send(){
    if(!$id.val()){
        showWarnModal("아이디를 입력해주세요!");
        $id.next().fadeIn(500);
        return;
    }
    if(!$password.val()){
        showWarnModal("비밀번호를 입력해주세요!");
        $password.next().fadeIn(500);
        return;
    }
    document.loginForm.submit();
}

$id.on("blur", function(){
    $id.next().hide();
    if($id.val()){
        $id.next().fadeIn(500);
        showHelp($id, "pass.png");
    }
});

$password.on("blur", function(){
    $password.next().hide();
    if($password.val()){
        $password.next().fadeIn(500);
        showHelp($password, "pass.png");
    }
});

$password.on("blur", function(){

});

function showHelp($input, fileName){
    $input.next().attr("src", "/static/images/member" + fileName);

    if(fileName == "pass.png") {
        $input.css("border", "1px solid rgba(0, 0, 0, 0.1)");
        $input.css("background", "rgb(255, 255, 255)");
        $input.next().attr("width", "18px");
    }else {
        $input.css("border", "1px solid rgb(255, 64, 62)");
        $input.css("background", "rgb(255, 246, 246)");
    }
}

$(".enterprise-login").on("click", function(){
    $(".iam-account-app").css("display", "none");
    $(".company").css("display", "block");
    $(".general").css("display", "none");
})

$(".member-login").on("click", function(){
    $(".iam-account-app").css("display", "block");
    $(".company").css("display", "none");
    $(".general").css("display", "block");
})
