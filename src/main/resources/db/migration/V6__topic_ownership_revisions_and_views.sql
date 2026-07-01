ALTER TABLE posts ADD COLUMN user_id BIGINT;
ALTER TABLE posts ADD COLUMN updated_at TIMESTAMPTZ;
ALTER TABLE posts ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0;

UPDATE posts
SET user_id = (SELECT id FROM users WHERE username = 'admin' ORDER BY id LIMIT 1),
    updated_at = created_at
WHERE user_id IS NULL;

ALTER TABLE posts ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE posts ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE posts ADD CONSTRAINT fk_posts_user FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE posts ADD CONSTRAINT uq_posts_title UNIQUE (title);

ALTER TABLE replies ADD COLUMN user_id BIGINT;
ALTER TABLE replies ADD COLUMN updated_at TIMESTAMPTZ;

UPDATE replies
SET user_id = (SELECT id FROM users WHERE username = 'admin' ORDER BY id LIMIT 1),
    updated_at = created_at
WHERE user_id IS NULL;

ALTER TABLE replies ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE replies ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE replies ADD CONSTRAINT fk_replies_user FOREIGN KEY (user_id) REFERENCES users (id);

CREATE INDEX idx_replies_post_id_created_at ON replies (post_id, created_at);
