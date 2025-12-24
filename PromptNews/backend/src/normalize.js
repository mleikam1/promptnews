export function normalizeSportsResults(payload = {}) {
  const sportsResults = payload.sports_results ?? {};
  const title = sportsResults.title ?? payload.search_information?.query_displayed ?? null;
  const ranking = normalizeRanking(sportsResults.rankings);
  const thumbnail = sportsResults.thumbnail ?? sportsResults.thumbnail_url ?? null;

  const games = flattenGames(sportsResults);

  const normalizedGames = games.map((game) => normalizeGame(game, sportsResults));
  const liveGame = normalizedGames.find((game) => isLiveGame(game));
  const recentGames = normalizedGames.filter((game) => !isLiveGame(game) && isRecentGame(game));
  const upcomingGames = normalizedGames.filter((game) => !isLiveGame(game) && !isRecentGame(game));

  return {
    team_overview: {
      title,
      ranking,
      thumbnail
    },
    live_game: liveGame ?? null,
    recent_games: recentGames,
    upcoming_games: upcomingGames
  };
}

function normalizeRanking(rankings) {
  if (!rankings) return null;
  if (Array.isArray(rankings)) {
    const top = rankings[0] ?? {};
    return (
      top.rank ??
      top.position ??
      top.name ??
      top.title ??
      (typeof top === "string" ? top : null)
    );
  }
  if (typeof rankings === "string") return rankings;
  if (typeof rankings === "object") {
    return rankings.rank ?? rankings.position ?? rankings.title ?? null;
  }
  return null;
}

function flattenGames(sportsResults) {
  const buckets = [
    sportsResults.live_games,
    sportsResults.recent_games,
    sportsResults.upcoming_games,
    sportsResults.games,
    sportsResults.events
  ];
  const games = [];
  for (const bucket of buckets) {
    if (Array.isArray(bucket)) {
      games.push(...bucket);
    }
  }
  return games;
}

function normalizeGame(game = {}, sportsResults = {}) {
  const teams = normalizeTeams(game);
  const score = normalizeScore(game, teams);
  const highlights = normalizeHighlights(game);

  return {
    date: game.date ?? game.start_date ?? game.game_date ?? null,
    time: game.time ?? game.start_time ?? game.game_time ?? null,
    league: game.tournament ?? game.league ?? game.sport ?? sportsResults.league ?? null,
    status: game.status ?? game.stage ?? game.result ?? null,
    teams,
    score,
    video_highlights: highlights
  };
}

function normalizeTeams(game) {
  if (Array.isArray(game.teams)) {
    return game.teams.map((team) => ({
      name: team.name ?? team.team_name ?? null,
      score: team.score ?? team.points ?? null,
      thumbnail: team.thumbnail ?? team.logo ?? null
    }));
  }
  const team1 = game.team1 ?? game.home_team;
  const team2 = game.team2 ?? game.away_team;
  return [team1, team2]
    .filter(Boolean)
    .map((team) => ({
      name: team.name ?? team.team_name ?? null,
      score: team.score ?? team.points ?? null,
      thumbnail: team.thumbnail ?? team.logo ?? null
    }));
}

function normalizeScore(game, teams) {
  if (game.score) return game.score;
  if (game.result) return game.result;
  if (teams.length >= 2) {
    const [home, away] = teams;
    if (home.score != null && away.score != null) {
      return `${home.score}-${away.score}`;
    }
  }
  return null;
}

function normalizeHighlights(game) {
  const highlights = game.video_highlights ?? game.highlights ?? {};
  if (!highlights || typeof highlights !== "object") return null;
  const link = highlights.link ?? highlights.url ?? null;
  const thumbnail = highlights.thumbnail ?? highlights.image ?? null;
  if (!link && !thumbnail) return null;
  return { link, thumbnail };
}

function isLiveGame(game) {
  if (game.status == null) return false;
  const status = String(game.status).toLowerCase();
  return status.includes("live") || status.includes("in progress") || status.includes("in-play");
}

function isRecentGame(game) {
  if (game.status == null) return game.score != null;
  const status = String(game.status).toLowerCase();
  return (
    status.includes("final") ||
    status.includes("ended") ||
    status.includes("ft") ||
    status.includes("full") ||
    status.includes("completed")
  );
}

export const __private__ = {
  normalizeRanking,
  normalizeTeams,
  normalizeScore,
  normalizeHighlights,
  isLiveGame,
  isRecentGame,
  flattenGames
};
