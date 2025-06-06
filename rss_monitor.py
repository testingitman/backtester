import hashlib
import json
import time
import random
import logging

import feedparser
import openai
import redis
import requests
import yaml

CONFIG = yaml.safe_load(open('config.yaml'))

openai.api_key = CONFIG.get('groq_api_key')
openai.base_url = "https://api.groq.com/openai/v1"
TELEGRAM_TOKEN = CONFIG.get('telegram_token')
TELEGRAM_CHAT = CONFIG.get('telegram_chat_id')
RSS_URL = CONFIG.get('rss_feed_url')

r = redis.Redis(host='localhost', port=6379, decode_responses=True)

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

PROMPT = (
    "Analyze this news item and respond with JSON. "
    "Fields: tokens (list of affected NSE symbols), action (Buy or Sell), "
    "confidence (0-10), term ('short' or 'long' for expected profit horizon), "
    "reason. Use concise JSON only. News: '{title} - {description}'."
)


def send_telegram(message: str):
    url = f"https://api.telegram.org/bot{TELEGRAM_TOKEN}/sendMessage"
    requests.post(url, data={'chat_id': TELEGRAM_CHAT, 'text': message})


def process_entry(entry):
    h = hashlib.sha1(entry.link.encode()).hexdigest()
    try:
        if r.exists(f"headline:{h}"):
            return
    except redis.exceptions.ConnectionError as e:
        logger.error("Redis not available", exc_info=e)
        return

    prompt = PROMPT.format(title=entry.title, description=entry.description)
    resp = openai.ChatCompletion.create(
        model="gpt-4",
        messages=[{"role": "user", "content": prompt}]
    )
    text = resp.choices[0].message.content.strip()
    try:
        data = json.loads(text)
    except json.JSONDecodeError:
        data = {"error": "Failed to parse", "raw": text}
    stored = {
        "title": entry.title,
        "link": entry.link,
        "analysis": data,
        "timestamp": time.time(),
        "close": round(90 + random.random() * 20, 2),
    }
    try:
        r.set(f"headline:{h}", json.dumps(stored))
    except redis.exceptions.ConnectionError as e:
        logger.error("Redis not available", exc_info=e)
        return
    send_telegram(f"{entry.title}\n{json.dumps(data, ensure_ascii=False)}")


if __name__ == "__main__":
    while True:
        try:
            feed = feedparser.parse(RSS_URL)
            for e in feed.entries:
                process_entry(e)
        except Exception as e:
            logger.exception("Error processing feed", exc_info=e)
        time.sleep(300)

