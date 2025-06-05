import hashlib
import json
import time

import feedparser
import openai
import redis
import requests
import yaml

CONFIG = yaml.safe_load(open('config.yaml'))

openai.api_key = CONFIG.get('openai_api_key')
TELEGRAM_TOKEN = CONFIG.get('telegram_token')
TELEGRAM_CHAT = CONFIG.get('telegram_chat_id')
RSS_URL = CONFIG.get('rss_feed_url')

r = redis.Redis(host='localhost', port=6379, decode_responses=True)

PROMPT = (
    "Analyze this news for trading sentiment: '{title} - {description}'. "
    "Return JSON with: company, sentiment (positive/neutral/negative), action "
    "(Buy/Hold/Sell), reason. Get confidence score on a scale of 10."
)


def send_telegram(message: str):
    url = f"https://api.telegram.org/bot{TELEGRAM_TOKEN}/sendMessage"
    requests.post(url, data={'chat_id': TELEGRAM_CHAT, 'text': message})


def process_entry(entry):
    h = hashlib.sha1(entry.link.encode()).hexdigest()
    if r.exists(f"headline:{h}"):
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
    stored = {"title": entry.title, "link": entry.link, "analysis": data}
    r.set(f"headline:{h}", json.dumps(stored))
    send_telegram(f"{entry.title}\n{json.dumps(data, ensure_ascii=False)}")


if __name__ == "__main__":
    while True:
        feed = feedparser.parse(RSS_URL)
        for e in feed.entries:
            process_entry(e)
        time.sleep(300)

