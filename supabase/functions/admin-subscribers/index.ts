// Read-only endpoint for the admin panel HTML page. Gated by a shared
// secret (ADMIN_PANEL_SECRET, a Supabase secret) instead of real user
// auth -- adequate for a single-admin internal tool, not for a
// multi-operator product. Uses the service role key to read past RLS,
// same reasoning as register-subscriber: nothing public talks to Postgres
// directly.

import { createClient } from "jsr:@supabase/supabase-js@2";

const supabase = createClient(
  Deno.env.get("SUPABASE_URL")!,
  Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
);

function cors(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json",
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Headers": "authorization, content-type, x-admin-secret",
      "Access-Control-Allow-Methods": "GET, OPTIONS",
    },
  });
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return cors({});

  const expected = Deno.env.get("ADMIN_PANEL_SECRET");
  const provided = req.headers.get("x-admin-secret");
  if (!expected || provided !== expected) {
    return cors({ error: "No autorizado" }, 401);
  }

  const { data, error } = await supabase
    .from("subscribers")
    .select("*")
    .order("created_at", { ascending: false });

  if (error) {
    return cors({ error: error.message }, 500);
  }

  return cors({ subscribers: data });
});
