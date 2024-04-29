$('.event-price').each(function() {
    let price = $(this).text();
    price = parseInt(price);
    price = price.toLocaleString();
    $(this).text(price);
});


const eventBoardSearch = {
    boardTitle: null,
    categoryType: null
};

$(".categoryType").on("click", function () {
    eventBoardSearch.categoryType ='ALL';
    eventBoardSearch.boardTitle = null;
    page = 0;
    let val = $(this).attr('value');
    let result;
    result = val;
    console.log("선택한 value : " + result);
    eventBoardSearch.categoryType = result;
    // 모든 게시글 지우기
    $(".categoryType").removeClass('active')
    $(this).addClass('active')
    $('.instance').remove()
    boardService.getList(appendList);
    $(window).scroll(function() {
        if($(window).scrollTop() + $(window).height() == Math.floor($(document).height() * 0.9)) {
            boardService.getList(appendList);
            bindLikeButtonClickEvent()
            page++
            console.log(page)
        }
    });
});





$(".search").on("change", function () {
    $(".search").val("");
});






$("form[name='search-form']").on("submit", function (e) {
    e.preventDefault();
    let val;
    let $search = $(".event-search-input").val();
    if ($search === "") return;
    console.log($search)
    val = $search;
    eventBoardSearch.boardTitle = val;
    console.log(eventBoardSearch.boardTitle + "888");
    // 모든 게시글 지우기
    $('.instance').remove()
    boardService.getList(appendList);
});


let page = 0;
const boardService = (() => {
    function getList(callback){
        $.ajax({
            url: `/event/list?page=${page}`,
            type: 'post',
            data: JSON.stringify(eventBoardSearch),
            contentType: "application/json;charset=utf-8",
            success: function(eventListDTOJSON){
                let eventListDTO = JSON.parse(eventListDTOJSON)
                if (eventListDTO.length === 0) { // 불러올 데이터가 없으면
                    console.log("막힘")
                    $(window).off('scroll'); // 스크롤 이벤트를 막음
                    return;
                }
                if(callback){
                    callback(eventListDTO);
                    $('.wish-button').on('click', function () {
                        handleLikeButtonClick(this);
                        console.log("좋아요 눌림 !");
                    });
                }
            }
        });
    }
    return {getList: getList};
})();

function appendList(eventListDTO) {
    let boardText3 = '';
    let mainFile = '';
    console.log(eventListDTO);
    eventListDTO.forEach((e,i) => {
        e.files.forEach((e1,j) => {
            if(typeof(e1.fileStatus) !== 'undefined'){
                mainFile =  `style="background-image:url('/eventFiles/display?fileName=Event/${e1.filePath}/${e1.fileUUID}_${e1.fileOriginalName}')"></div>`
            }
        })
        boardText3 +=  `
             <div role="presentation" class="instance">
                            <a class="item" href="/event/detail/${e.id}">
                                <div class="thumbnail-container">
                                    <div class="thumbnail-list">
                                        <div class="photo-thumbnail"
                                        ${mainFile}
<!--                                             style="background-image:url('/eventFiles/display?fileName=Event/${e.files[0].filePath}/${e.files[0].fileUUID}_${e.files[0].fileOriginalName}')"></div>-->
                                        <!-- 사진 div -->
                                    </div>
                                </div>
                                <div class="list-content">
                                    <div class="list-title">
                                        ${e.boardTitle}
                                    </div>
                                    <div class="for-price-full-contain">
                                        <div class="for-price-wrap">
                                            <div class="list-writer">${e.memberNickname}</div>
                                            <div class="list-date-container">
                                                        <span class="print-data"
                                                        >${e.eventLocation.address}</span
                                                        >
                                            </div>
                                        </div>
                                        <span class="event-price-wrap">
                                                    <span class="event-price">${e.eventPrice}</span
                                                    ><span>원</span>
                                                </span>
                                    </div>
                                    <div clsas="list-footer">
                                        <div class="list-footer-container">
                                            <div class="loca-status-container">
                                                <div class="status-community">
                                                    <i class="second-confirm"></i>
                                                    <span class="ing">
                                                                <span class="event-start-day"
                                                                >${convertDateFormat(e.calendar.startDate)}</span
                                                                >
                                                                ~<span class="event-end-day"
                                                    >${convertDateFormat(e.calendar.endDate)}</span
                                                    >
                                                                <div class="like-count-container">
                                                                    <div class="people-icon"></div>
                                                                    <span class="like-count"
                                                                    >${e.eventRecruitCount}</span
                                                                    >
                                                                    <span>명 모집</span>
                                                                </div>
                                                            </span>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </a>
                            <button type="button" class="wish-button table${e.id}" aria-pressed="${e.isEventLiked}" eventId="${e.id}" >
                                <svg
                                        viewBox="0 0 32 32"
                                        focusable="false"
                                        role="presentation"
                                        class="wish-button-svg"
                                        aria-hidden="true"
                                >
                                    <path
                                            d="M22.16 4h-.007a8.142 8.142 0 0 0-6.145 2.79A8.198 8.198 0 0 0 9.76 3.998a7.36 7.36 0 0 0-7.359 7.446c0 5.116 4.64 9.276 11.6 15.596l2 1.76 2-1.76c6.96-6.32 11.6-10.48 11.6-15.6v-.08A7.36 7.36 0 0 0 22.241 4h-.085z"
                                    ></path>
                                </svg>
                            </button>
                        </div>
                        `
        ;
    });
    if (eventListDTO.length === 0) { // 불러올 데이터가 없으면
        $(window).off('scroll'); // 스크롤이벤트 x
    }
    $('.collection-table').append(boardText3);
}

// 페이지 로딩 시 초기 리스트를 불러옴
boardService.getList(function(eventListDTO) {
    boardService.getList(appendList);
});


$(window).scroll(function() {
    if(Math.ceil($(window).scrollTop() + $(window).height()) == Math.floor($(document).height() * 0.9)) {
        page++;
        boardService.getList(appendList);
        bindLikeButtonClickEvent()
        console.log(page)
    }
});

bindLikeButtonClickEvent()

/*좋아요 버튼*/
/* 좋아요 버튼 */
$(function () {
    $('.wish-button').click(function (e) {
        let target = $(e.target);
        if ($(target).attr('aria-pressed') == 'false') {
            $(target).attr('aria-pressed', 'true'); //하트 색 채우기
            $('.like-cancel-text').hide(); //해제 문구
            $('#like-modal').css({ right: '-30%' }); //오->왼 슬라이드 등장
            $('#like-modal').animate({ right: '30px' }, { duration: 500 }); //오->왼 슬라이드 등장
            $('#like-modal').show(); //슬라이드 보이기
            $('.like-text').show(); //찜 추가 문구
            $('#like-modal').fadeOut(); //사라지기
        } else {
            $(target).attr('aria-pressed', 'false'); //색 비우기
            $('.like-text').hide(); //찜 추가 문구
            $('#like-modal').css({ right: '-30%' }); //오->왼 슬라이드 등장
            $('#like-modal').animate({ right: '30px' }, { duration: 500 }); //오->왼 슬라이드 등장
            $('#like-modal').show(); //슬라이드 보이기
            $('.like-cancel-text').show(); //찜 해제 문구
            $('#like-modal').fadeOut(); //사라지기
        }
    });
});

/* top 버튼  */
var topEle = $('.top-btn');
var delay = 400;
console.log(topEle);

/* top show-hide */
$(window).scroll(function () {
    if ($(this).scrollTop() > 200) {
        $('.top-btn').fadeIn();
    } else {
        $('.top-btn').fadeOut();
    }
});

/* top버튼- 위로 올리기 */
$('.top-btn').click(function () {
    $('html, body').animate({ scrollTop: 0 }, delay);
    return false;
});

// 좋아요 버튼 이벤트 핸들러 바인딩 함수
function bindLikeButtonClickEvent() {
    $('.wish-button').off('click').on('click', function () {
        handleLikeButtonClick(this);
    });
}
function handleLikeButtonClick(element) {
    let eventString = $(element).attr('eventId');
    let eventId = parseInt(eventString);

    let lsLikeString = $(element).attr('aria-pressed');
    let isLike = (lsLikeString === "true");
    $.ajax({
        url: '/eventLikes/save',
        type: 'POST',
        data: { "eventId": eventId, "isLike": isLike },
        success: function(response) {
            if ($(element).attr('aria-pressed') == 'false') {
                $(element).attr('aria-pressed', 'true'); // 하트 색 채우기
                $('.like-cancel-text').hide(); // 해제 문구
                $('#like-modal').css({ right: '-30%' }); // 오->왼 슬라이드 등장
                $('#like-modal').animate({ right: '30px' }, { duration: 1000 }); // 오->왼 슬라이드 등장
                $('#like-modal').show(); // 슬라이드 보이기
                $('#like-modal').css({ display: 'flex' });
                $('#like-modal').css({ 'align-items': 'center' });
                $('.like-text').show(); // 찜 추가 문구
                $('#like-modal').stop().fadeOut(); // 사라지기 (애니메이션 중지 후 사라짐)
            } else {
                $(element).attr('aria-pressed', 'false'); // 색 비우기
                $('.like-text').hide(); // 찜 추가 문구
                $('#like-modal').css({ right: '-30%' }); // 오->왼 슬라이드 등장
                $('#like-modal').animate({ right: '30px' }, { duration: 1000 }); // 오->왼 슬라이드 등장
                $('#like-modal').show(); // 슬라이드 보이기
                $('.like-cancel-text').show(); // 찜 해제 문구
                $('#like-modal').stop().fadeOut(); // 사라지기 (애니메이션 중지 후 사라짐)
            }
        },
    });
}
















function convertDateFormat(dateString) {
    let dateParts = dateString.split('T')[0].split('-');
    let year = dateParts[0];
    let month = dateParts[1];
    let day = dateParts[2];

    let formattedDate = year + '.' + month + '.' + day;
    return formattedDate;
}
