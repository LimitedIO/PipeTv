"""Builds banner/social SVGs (TV mark matches logo.svg). Run from docs/assets: python3 src/brand.py"""
import re, random
def tv(x, y, size, pfx):
    return f'''<svg x="{x}" y="{y}" width="{size}" height="{size}" viewBox="0 0 512 512"><defs><linearGradient id="{pfx}g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#1fd1a5"/><stop offset="1" stop-color="#8a1cf0"/></linearGradient></defs>
<rect x="92" y="172" width="328" height="236" rx="88" fill="#0b0418" opacity=".45"/>
<g stroke="#fff" stroke-width="26" stroke-linecap="round"><path d="M222 156 180 96"/><path d="M290 156 332 96"/></g>
<rect x="92" y="156" width="328" height="236" rx="88" fill="#fff"/>
<path d="M226 222v108l92-54z" fill="url(#{pfx}g)" stroke="url(#{pfx}g)" stroke-width="28" stroke-linejoin="round"/></svg>'''

def scene(w, h, horizon, sx, sr):
    random.seed(7)
    stars = ''.join(f'<circle cx="{random.randint(0, w)}" cy="{random.randint(0, int(horizon * .85))}" r="{random.choice([1, 1.4, 2, 2.6])}" opacity="{random.choice([.4, .6, .9])}"/>' for _ in range(int(w * h / 9000)))
    vx = w * .5; lines = ''
    for i in range(-16, 17):
        lines += f'<path d="M{vx + i * 18:.0f} {horizon}L{vx + i * w * .16:.0f} {h}"/>'
    y, d = horizon, 6
    while y < h:
        lines += f'<path d="M0 {y:.0f}H{w}"/>'; y += d; d *= 1.35
    cuts = ''.join(f'<rect y="{horizon - sr * .62 + k * k * sr * .012 + k * sr * .07:.0f}" width="{w}" height="{sr * (.014 + k * .011):.1f}"/>' for k in range(8))
    return f'''<defs><linearGradient id="sky" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#0b0a24"/><stop offset=".6" stop-color="#1d1260"/><stop offset="1" stop-color="#3b1a8f"/></linearGradient>
<linearGradient id="ssun" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#1fd1a5"/><stop offset=".55" stop-color="#5b3df5"/><stop offset="1" stop-color="#8a1cf0"/></linearGradient>
<linearGradient id="floor" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#170c45"/><stop offset="1" stop-color="#07061a"/></linearGradient>
<linearGradient id="word" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#ffffff"/><stop offset=".5" stop-color="#d8fff4"/><stop offset=".52" stop-color="#1fd1a5"/><stop offset="1" stop-color="#7a5cff"/></linearGradient>
<mask id="sm"><rect width="{w}" height="{h}" fill="#fff"/><g fill="#000">{cuts}</g></mask></defs>
<rect width="{w}" height="{h}" fill="url(#sky)"/><g fill="#fff">{stars}</g>
<circle cx="{sx}" cy="{horizon}" r="{sr}" fill="url(#ssun)" mask="url(#sm)"/>
<rect y="{horizon}" width="{w}" height="{h - horizon}" fill="url(#floor)"/>
<g stroke="#7a5cff" stroke-width="2" opacity=".55">{lines}</g>
<rect y="{horizon - 2}" width="{w}" height="3" fill="#6bffe6" opacity=".9"/>'''

def word(x, y, size, sh):
    return f'''<g font-family="Righteous"><text x="{x}" y="{y}" font-size="{size}" fill="#0b0a24" transform="translate({sh} {sh * 1.2})">PipeTv</text>
<text x="{x}" y="{y}" font-size="{size}" fill="url(#word)" stroke="#0b0a24" stroke-width="{sh * .6}" paint-order="stroke">PipeTv</text></g>'''

open('banner.svg', 'w').write(f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 640 360">{scene(640, 360, 262, 150, 150)}
{tv(18, 40, 250, 'b')}{word(282, 192, 104, 6)}
<text x="290" y="234" font-family="VT323" font-size="34" fill="#6bffe6" letter-spacing="2">YOUTUBE · BIG SCREEN</text>
</svg>''')

open('social.svg', 'w').write(f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1280 640">{scene(1280, 640, 480, 300, 260)}
{tv(50, 60, 470, 's')}{word(540, 290, 176, 9)}
<text x="550" y="362" font-family="Outfit" font-weight="700" font-size="38" fill="#ffffff">YouTube on your TV, like it used to feel.</text>
<text x="550" y="420" font-family="VT323" font-size="36" fill="#6bffe6">NO ADS · NO ACCOUNT · NO GOOGLE · FREE &amp; OPEN</text>
<text x="1240" y="612" text-anchor="end" font-family="VT323" font-size="34" fill="#9fb4ff">limitedio.github.io/PipeTv</text>
</svg>''')
