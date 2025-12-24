import express from "express";
import { normalizeSportsResults } from "./normalize.js";

const app = express();
const port = process.env.PORT || 8080;

app.get("/api/sports", async (req, res) => {
  const query = req.query.s?.toString().trim();
  if (!query) {
    return res.status(400).json({ error: "Missing query param: s" });
  }

  const apiKey = process.env.SERPAPI_KEY;
  if (!apiKey) {
    return res.status(500).json({ error: "SERPAPI_KEY is not configured" });
  }

  const url = new URL("https://serpapi.com/search.json");
  url.searchParams.set("engine", "google_sports_results");
  url.searchParams.set("q", query);
  url.searchParams.set("hl", "en");
  url.searchParams.set("gl", "us");
  url.searchParams.set("api_key", apiKey);

  try {
    const response = await fetch(url);
    if (!response.ok) {
      return res.status(502).json({
        error: "SerpApi request failed",
        status: response.status
      });
    }
    const payload = await response.json();
    const normalized = normalizeSportsResults(payload);
    return res.json(normalized);
  } catch (error) {
    return res.status(500).json({ error: "Failed to load sports results" });
  }
});

app.listen(port, () => {
  console.log(`PromptNews sports backend listening on ${port}`);
});
