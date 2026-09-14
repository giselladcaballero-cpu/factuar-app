// Read-only endpoint for the admin panel HTML page.
//
// TEMPORARY: the ADMIN_PANEL_SECRET gate is disabled below (commented out)
// while there are no real subscribers yet and the panel is being set up.
// Re-enable it before this table has real merchant data -- uncomment the
// check and set ADMIN_PANEL_SECRET as a Supabase secret.
//
// Uses the service role key to read past RLS: nothing public gets direct
// Postgres access, only this function.

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

  // const expected = Deno.env.get("ADMIN_PANEL_SECRET");
  // const provided = req.headers.get("x-admin-secret");
  // if (!expected || provided !== expected) {
  //   return cors({ error: "No autorizado" }, 401);
  // }

  const { data, error } = await supabase
    .from("subscribers")
    .select("*")
    .order("created_at", { ascending: false });

  if (error) {
    return cors({ error: error.message }, 500);
  }

  return cors({ subscribers: data });
});
