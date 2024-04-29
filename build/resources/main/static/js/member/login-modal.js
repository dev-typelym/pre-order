/* login-modal */

let modalCheck;
function showWarnModal(modalMessage){
    modalCheck = false;
    $("div#content-wrap").html(modalMessage)
    $("div.warn-modal").css("animation", "popUp 0.5s");
    $("div.modal").css("display", "flex").hide().fadeIn(500);
    setTimeout(function(){modalCheck = true;}, 500);
}

$("div.modal").on("click", function(){
    if(modalCheck){
        $("div.warn-modal").css("animation", "popDown 0.5s");
        $("div.modal").fadeOut(500);
    }
});
const $passwordInput = $("#password-input"); //비밀번호
const $passwordCheckInput = $(".re-input-password-container");//비밀번호체크


$("#eye1").on("click",function(){
    $passwordInput.type = 'text';
})