# PromptNews

## Sports Scores Feature

PromptNews now includes a dedicated Sports Scores experience that pulls structured results from SerpApi and renders them inside the Android app.

### Backend (Sports API)

The backend service proxies SerpApi Google Sports Results and normalizes the payload.

```bash
cd PromptNews/backend
npm install
SERPAPI_KEY=your_key_here npm start
```

The service runs on `http://localhost:8080` by default and exposes:

```
GET /api/sports?s={team_or_sport_query}
```

### Android UI Usage

1. Build/run the Android app.
2. Open the **Sports** tab.
3. Search for a team or sport to load live games, recent results, upcoming matchups, and stats.

### SerpApi Key Configuration

- **Backend**: set `SERPAPI_KEY` in your environment before running the server.
- **Android**: provide `SPORTS_API_BASE_URL` in your Gradle properties (or use the emulator default of `http://10.0.2.2:8080`).

Example `gradle.properties`:

```
SERPAPI_KEY=your_serpapi_key
SPORTS_API_BASE_URL=http://10.0.2.2:8080
```

### Data Fields Used (SerpApi)

From `sports_results`:

- `sports_results.title`
- `sports_results.rankings`
- `sports_results.thumbnail`

For each game:

- `tournament`, `stage`
- `date`, `status`
- `teams[].name`, `teams[].score`, `teams[].thumbnail`
- `video_highlights.link`, `video_highlights.thumbnail`
