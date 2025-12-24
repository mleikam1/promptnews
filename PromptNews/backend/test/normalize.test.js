import test from "node:test";
import assert from "node:assert/strict";
import { normalizeSportsResults } from "../src/normalize.js";

const fixture = {
  sports_results: {
    title: "Los Angeles Lakers",
    rankings: [{ rank: "#2 West" }],
    thumbnail: "https://example.com/lakers.png",
    games: [
      {
        date: "2024-04-01",
        time: "7:00 PM",
        tournament: "NBA",
        status: "Final",
        teams: [
          { name: "Lakers", score: "110", thumbnail: "lakers.png" },
          { name: "Suns", score: "102", thumbnail: "suns.png" }
        ],
        video_highlights: { link: "https://example.com/highlights" }
      }
    ]
  }
};

test("normalizeSportsResults shapes core fields", () => {
  const result = normalizeSportsResults(fixture);

  assert.equal(result.team_overview.title, "Los Angeles Lakers");
  assert.equal(result.team_overview.ranking, "#2 West");
  assert.equal(result.team_overview.thumbnail, "https://example.com/lakers.png");

  assert.equal(result.live_game, null);
  assert.equal(result.recent_games.length, 1);
  assert.equal(result.recent_games[0].league, "NBA");
  assert.equal(result.recent_games[0].score, "110-102");
  assert.equal(result.recent_games[0].teams.length, 2);
});
