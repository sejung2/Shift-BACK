-- ============================================
-- 도커 init 스크립트는 SYS 계정으로 CDB 루트에서 실행됨
-- 1) TEST 계정이 있는 PDB(XEPDB1)로 이동
-- 2) 테이블이 TEST 계정 소유로 생성되도록 기본 스키마 지정
-- ============================================
-- 3) SQL*Plus는 기본적으로 빈 줄을 만나면 문장 입력을 끊음
--    (reviews, chatroom_users 등 테이블 정의 중간에 빈 줄이 있어서 필요)
SET
SQLBLANKLINES ON

ALTER
SESSION SET CONTAINER = XEPDB1;
ALTER
SESSION SET CURRENT_SCHEMA = TEST;

-- ============================================
-- Oracle DDL - 쇼핑몰 데이터베이스
-- ============================================

-- 1. 사용자 테이블
CREATE TABLE users
(
    user_id       NUMBER(8) PRIMARY KEY,
    login_id      VARCHAR2(20) NOT NULL UNIQUE,
    password      VARCHAR2(100) NOT NULL,
    name          VARCHAR2(20) NOT NULL,
    phone         VARCHAR2(20) UNIQUE,
    address       VARCHAR2(200),
    points        NUMBER(10) DEFAULT 0,
    refresh_token VARCHAR2(255),
    admin_flag    CHAR(1)   DEFAULT 'N' CHECK (admin_flag IN ('Y', 'N')),
    deleted_at    TIMESTAMP DEFAULT NULL,
    version       NUMBER(10) DEFAULT 0 NOT NULL
);

-- 2. 친구 테이블
CREATE TABLE friends
(
    friendship_id NUMBER(8) PRIMARY KEY,
    user_id       NUMBER(8) NOT NULL,
    friend_id     NUMBER(8) NOT NULL,
    CONSTRAINT fk_friends_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_friends_friend FOREIGN KEY (friend_id)
        REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT chk_friends_not_self CHECK (user_id != friend_id
)
    );

-- 3. 카테고리 테이블
CREATE TABLE categories
(
    category_id   NUMBER(8) PRIMARY KEY,
    category_name VARCHAR2(50) NOT NULL
);

-- 4. 상품 테이블
CREATE TABLE products
(
    product_id        NUMBER(8) PRIMARY KEY,
    category_id       NUMBER(8) NOT NULL,
    product_name      VARCHAR2(150) NOT NULL,
    price             NUMBER(10) NOT NULL,
    stock             NUMBER(8) NOT NULL,
    registration_date DATE DEFAULT SYSDATE,
    seller            VARCHAR2(30),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id)
        REFERENCES categories (category_id)
);

-- 5. 이미지 테이블
CREATE TABLE images
(
    image_id          NUMBER(8) PRIMARY KEY,
    product_id        NUMBER(8) NOT NULL,
    image_url         VARCHAR2(100) NOT NULL,
    is_representative CHAR(1) DEFAULT 'N' CHECK (is_representative IN ('Y', 'N')),
    CONSTRAINT fk_images_product FOREIGN KEY (product_id)
        REFERENCES products (product_id) ON DELETE CASCADE
);

-- 6. 장바구니 테이블
CREATE TABLE cart_items
(
    cart_items_id NUMBER(8) PRIMARY KEY,
    product_id    NUMBER(8) NOT NULL,
    user_id       NUMBER(8) NOT NULL,
    quantity      NUMBER(8) NOT NULL,
    price         NUMBER(10) NOT NULL,
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id)
        REFERENCES products (product_id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE CASCADE
);

-- 7. 주문 테이블
CREATE TABLE orders
(
    order_id      NUMBER(8) PRIMARY KEY,
    sender_id     NUMBER(8) NOT NULL,
    receiver_id   NUMBER(8) NOT NULL,
    order_date    DATE DEFAULT SYSDATE,
    total_price   NUMBER(20) NOT NULL,
    point_used    NUMBER(20) DEFAULT 0,
    remain_points NUMBER(20) DEFAULT 0,
    cash_used     NUMBER(20) DEFAULT 0,
    order_status  VARCHAR2(2) DEFAULT 'P' CHECK (order_status IN ('P', 'S', 'D', 'C')),
    version       NUMBER(10) DEFAULT 0 NOT NULL,
    CONSTRAINT fk_orders_sender FOREIGN KEY (sender_id)
        REFERENCES users (user_id),
    CONSTRAINT fk_orders_receiver FOREIGN KEY (receiver_id)
        REFERENCES users (user_id)
);


-- 8. 주문상품 테이블 (주문 상세)
CREATE TABLE order_items
(
    order_item_id NUMBER(8) PRIMARY KEY,
    order_id      NUMBER(8) NOT NULL,
    product_id    NUMBER(8) NOT NULL,
    quantity      NUMBER(8) NOT NULL,
    item_price    NUMBER(10) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id)
        REFERENCES orders (order_id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id)
        REFERENCES products (product_id)
);

-- 9. 배송 테이블
CREATE TABLE deliveries
(
    delivery_id      NUMBER(8) PRIMARY KEY,
    order_id         NUMBER(8) NOT NULL,    -- 주문 번호
    tracking_number  VARCHAR2(20) UNIQUE,   -- 송장 번호
    recipient        VARCHAR2(20) NOT NULL, -- 수령인
    recipient_phone  VARCHAR2(20) NOT NULL, -- (수령인) 핸드폰 번호
    delivery_address VARCHAR2(200) NOT NULL,
    delivery_status  CHAR(1) DEFAULT 'P' CHECK (delivery_status IN ('P', 'S', 'D', 'C')),
    estimated_date   DATE,
    request_message  VARCHAR2(30),
    CONSTRAINT fk_deliveries_order FOREIGN KEY (order_id)
        REFERENCES orders (order_id) ON DELETE CASCADE
);

-- 10. 리뷰 테이블
CREATE TABLE reviews
(
    review_id     NUMBER(8) PRIMARY KEY,
    user_id       NUMBER(8) NOT NULL,
    product_id    NUMBER(8) NOT NULL, -- product_id 유지
    order_item_id NUMBER(8) NOT NULL, -- order_item_id 추가
    rating        NUMBER(2) NOT NULL CHECK (rating BETWEEN 1 AND 5),
    content       VARCHAR2(500),
    created_date  DATE DEFAULT SYSDATE,

    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE CASCADE,

    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id)
        REFERENCES products (product_id) ON DELETE CASCADE,

    CONSTRAINT fk_reviews_order_item FOREIGN KEY (order_item_id)
        REFERENCES order_items (order_item_id) ON DELETE CASCADE
);


-- 11. 채팅방 테이블
CREATE TABLE chatrooms
(
    chatroom_id      NUMBER(8) PRIMARY KEY,
    last_msg_content VARCHAR2(300),
    last_msg_date    DATE
);

-- 12. 채팅방 유저 테이블
CREATE TABLE chatroom_users
(
    chatroom_users_id    NUMBER(8) PRIMARY KEY,
    chatroom_id          NUMBER(8) NOT NULL,
    user_id              NUMBER(8),
    chatroom_name        VARCHAR2(30 CHAR),
    created_time         DATE,
    last_connection_time DATE,
    connection_status    VARCHAR2(2) CHECK (connection_status IN ('ON', 'OF', 'DL')),
    is_dark_mode         CHAR(1) DEFAULT 'N' CHECK (is_dark_mode IN ('Y', 'N')),

    CONSTRAINT fk_chatroomUsers_chatroomId FOREIGN KEY (chatroom_id)
        REFERENCES chatrooms (chatroom_id) ON DELETE CASCADE,
    CONSTRAINT fk_chatroomUsers_userId FOREIGN KEY (user_id)
        REFERENCES users (user_id)
);

-- 13. 메시지 테이블
CREATE TABLE messages
(
    message_id   NUMBER(8) PRIMARY KEY,
    chatroom_id  NUMBER(8) NOT NULL,
    user_id      NUMBER(8),
    content      VARCHAR2(300) NOT NULL,
    is_gift      CHAR(1) DEFAULT 'N' CHECK (is_gift IN ('Y', 'N')) NOT NULL,
    send_date    DATE    DEFAULT SYSDATE,
    unread_count NUMBER(3) NOT NULL,

    CONSTRAINT fk_messages_chatroomId FOREIGN KEY (chatroom_id)
        REFERENCES chatrooms (chatroom_id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_userId FOREIGN KEY (user_id)
        REFERENCES users (user_id)
);

-- 14. 메세지 이모티콘 (메세지에 달리는 반응 이모티콘)
CREATE TABLE reply_emoticons
(
    reply_emoticon_id NUMBER(8) PRIMARY KEY,
    message_id        NUMBER(8) NOT NULL,
    user_id           NUMBER(8),
    type              VARCHAR2(3) CHECK (type IN ('HR', 'TH', 'SM')) NOT NULL,

    CONSTRAINT fk_replyEmoticons_messageId FOREIGN KEY (message_id)
        REFERENCES messages (message_id) ON DELETE CASCADE,
    CONSTRAINT fk_replyEmoticons_userId FOREIGN KEY (user_id)
        REFERENCES users (user_id)
);

-- 15. 문의 테이블
CREATE TABLE inquiries
(
    inquiry_id   NUMBER(8) PRIMARY KEY,
    user_id      NUMBER(8) NOT NULL,
    replier_id   NUMBER(8),
    title        VARCHAR2(30) NOT NULL,
    content      VARCHAR2(1000),
    created_date DATE DEFAULT SYSDATE,
    CONSTRAINT fk_inquiries_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_inquiries_replier FOREIGN KEY (replier_id)
        REFERENCES users (user_id)
);

-- 16. 문의 답변 테이블
CREATE TABLE inquiry_replies
(
    reply_id      NUMBER(8) PRIMARY KEY,
    inquiry_id    NUMBER(8) NOT NULL,
    reply_content VARCHAR2(300),
    replied_date  DATE DEFAULT SYSDATE,
    CONSTRAINT fk_inquiry_replies_inquiry FOREIGN KEY (inquiry_id)
        REFERENCES inquiries (inquiry_id) ON DELETE CASCADE
);

-- 17. 포인트 거래 테이블
CREATE TABLE point_transactions
(
    point_transaction_id NUMBER(8) PRIMARY KEY,
    user_id              NUMBER(8) NOT NULL,
    order_id             NUMBER(8),
    transaction_type     CHAR(1) CHECK (transaction_type IN ('A', 'U', 'R')),
    amount               NUMBER(20) NOT NULL,
    created_at           TIMESTAMP DEFAULT SYSTIMESTAMP,
    CONSTRAINT fk_pt_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_pt_order FOREIGN KEY (order_id)
        REFERENCES orders (order_id)
);


-- 18. 리프레시 토큰 테이블 (리프레시 토큰 개인 작업 때 추가)
CREATE TABLE refresh_tokens
(
    id          NUMBER(8) PRIMARY KEY,
    user_id     NUMBER(8) NOT NULL UNIQUE,
    token_value VARCHAR2(500) NOT NULL,
    expired_at  TIMESTAMP NOT NULL,
    CONSTRAINT fk_rt_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE CASCADE
);

-- ============================================
-- 시퀀스 생성 (자동 증가용)
-- INSERT INTO users (user_id, login_id, password, name, phone)
-- VALUES (seq_users.NEXTVAL, 'john123', 'pass123', '홍길동', '010-1234-5678');
-- 이런식으 사용하됨
-- ============================================

CREATE SEQUENCE seq_users START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_categories START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_products START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_images START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_cart_items START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_orders START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_order_items START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_deliveries START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_reviews START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_messages START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_chatrooms START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_chatroom_users START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_reply_emoticons START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_friends START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_inquiries START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_inquiry_replies START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_point_transactions START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_refresh_token START WITH 1 INCREMENT BY 1;

-- ============================================
-- 인덱스 생성 (성능 최적화)
-- 전체 테이블을 처음부터 끝까지 스캔하는 대신 책의 색인과 같이 B-Tree 구조로 빠르게 검색
-- ============================================

-- 사용자 로그인 조회 최적화(PRIMARY KEY로 이미 CREATE TABLE에서 생성됨)
-- CREATE INDEX idx_users_login_id ON users(login_id);

-- 상품 카테고리별 조회 최적화
CREATE INDEX idx_products_category ON products (category_id);

-- 주문 사용자별 조회 최적화
CREATE INDEX idx_orders_sender ON orders (sender_id);
CREATE INDEX idx_orders_receiver ON orders (receiver_id);

-- 리뷰 상품별 조회 최적화
CREATE INDEX idx_reviews_product ON reviews (product_id);
CREATE INDEX idx_reviews_order_item ON reviews (order_item_id);
CREATE INDEX idx_reviews_user ON reviews (user_id);

-- 메시지 채팅방별 조회 최적화
CREATE INDEX idx_messages_chatroom ON messages (chatroom_id);

-- 배송 주문별 조회 최적화
CREATE INDEX idx_deliveries_order ON deliveries (order_id);

-- 친구 관계 조회 최적화
CREATE INDEX idx_friends_user ON friends (user_id);
CREATE INDEX idx_friends_friend ON friends (friend_id);


-- ============================================
-- 주석 추가 (테이블 설명 문서화)
-- ============================================

COMMENT
ON TABLE users IS '사용자 정보';
COMMENT
ON TABLE categories IS '상품 카테고리';
COMMENT
ON TABLE products IS '상품 정보';
COMMENT
ON TABLE images IS '상품 이미지';
COMMENT
ON TABLE cart_items IS '장바구니';
COMMENT
ON TABLE orders IS '주문 정보';
COMMENT
ON TABLE order_items IS '주문 상세 항목';
COMMENT
ON TABLE deliveries IS '배송 정보';
COMMENT
ON TABLE reviews IS '상품 리뷰';
COMMENT
ON TABLE chatrooms IS '채팅방';
COMMENT
ON TABLE Chatroom_Users IS '채팅방 유저';
COMMENT
ON TABLE messages IS '채팅 메시지';
COMMENT
ON TABLE Reply_Emoticons IS '메시지 이모티콘';
COMMENT
ON TABLE friends IS '친구 관계';
COMMENT
ON TABLE inquiries IS '고객 문의';
COMMENT
ON TABLE inquiry_replies IS '문의 답변';