// Called by the app right after a subscription checkout resolves (see
// subscription-callback / BillingViewModel.completeSubscriptionActivation),
// so the admin panel has a central record of every merchant subscribed to
// Vektor Go. Uses the service role key (auto-provided to every Edge
// Function by Supabase, never a custom secret) to upsert past RLS -- the
// app itself never gets direct Postgres access.

import { createClient } from "jsr:@supabase/supabase-js@2";

const supabase = createClient(
  Deno.env.get("SUPABASE_URL")!,
  Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
);

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") {
    return new Response("Method not allowed", { status: 405 });
  }

  try {
    const body = await req.json();
    const {
      mp_collector_id,
      business_name = "",
      cuit = 0,
      email = "",
      subscription_id = "",
      subscription_status = "NONE",
      subscription_amount = 14999,
      environment = "HOMOLOGACION",
      app_version = "",
    } = body;

    if (!mp_collector_id) {
      return new Response(JSON.stringify({ error: "mp_collector_id es obligatorio" }), {
        status: 400,
        headers: { "Content-Type": "application/json" },
      });
    }

    const { error } = await supabase.from("subscribers").upsert(
      {
        mp_collector_id,
        business_name,
        cuit,
        email,
        subscription_id,
        subscription_status,
        subscription_amount,
        environment,
        app_version,
        last_synced_at: new Date().toISOString(),
      },
      { onConflict: "mp_collector_id" },
    );

    if (error) {
      return new Response(JSON.stringify({ error: error.message }), {
        status: 500,
        headers: { "Content-Type": "application/json" },
      });
    }

    return new Response(JSON.stringify({ ok: true }), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  } catch (e) {
    return new Response(JSON.stringify({ error: (e as Error).message }), {
      status: 500,
      headers: { "Content-Type": "application/json" },
    });
  }
});
