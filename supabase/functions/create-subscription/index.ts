// Starts a Vektor Go subscription checkout. The app opens this URL in the
// browser; it creates a Mercado Pago recurring preapproval against a plan
// that has NO free_trial configured (so the first charge happens the day
// the merchant authorizes it, not after any grace period), then redirects
// straight to Mercado Pago's own checkout for that preapproval.
//
// IMPORTANT — this charges INTO Vektor Go's own Mercado Pago account, not
// the merchant's. VEKTOR_BUSINESS_MP_ACCESS_TOKEN and
// VEKTOR_SUBSCRIPTION_PLAN_ID are placeholders until that separate business
// account exists; see supabase/README.md for the setup steps once it does.

const MP_PREAPPROVAL_URL = "https://api.mercadopago.com/preapproval";

Deno.serve(async (req: Request) => {
  const url = new URL(req.url);
  const payerEmail = url.searchParams.get("payer_email") ?? undefined;

  const businessToken = Deno.env.get("VEKTOR_BUSINESS_MP_ACCESS_TOKEN");
  const planId = Deno.env.get("VEKTOR_SUBSCRIPTION_PLAN_ID");
  const backUrl = Deno.env.get("VEKTOR_SUBSCRIPTION_BACK_URL"); // this function's own subscription-callback URL

  if (!businessToken || !planId || !backUrl) {
    return new Response(
      "Falta configurar VEKTOR_BUSINESS_MP_ACCESS_TOKEN, VEKTOR_SUBSCRIPTION_PLAN_ID o VEKTOR_SUBSCRIPTION_BACK_URL en los secrets de Supabase.",
      { status: 500, headers: { "Content-Type": "text/plain; charset=utf-8" } },
    );
  }

  try {
    const body: Record<string, unknown> = {
      preapproval_plan_id: planId,
      back_url: backUrl,
      status: "pending",
    };
    if (payerEmail) body.payer_email = payerEmail;

    const mpResponse = await fetch(MP_PREAPPROVAL_URL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${businessToken}`,
      },
      body: JSON.stringify(body),
    });

    const data = await mpResponse.json();

    if (!mpResponse.ok || !data.init_point) {
      return new Response(
        `Mercado Pago rechazó la creación de la suscripción: ${data.message || JSON.stringify(data)}`,
        { status: 502, headers: { "Content-Type": "text/plain; charset=utf-8" } },
      );
    }

    // Straight to Mercado Pago's own checkout for this preapproval — a
    // plain redirect, not an HTML page (see mp-callback's own notes on
    // why: Supabase's edge gateway sandboxes non-JSON HTML responses under
    // real browser Accept-Encoding headers).
    return Response.redirect(data.init_point, 302);
  } catch (e) {
    return new Response(
      `Error inesperado creando la suscripción: ${(e as Error).message}`,
      { status: 500, headers: { "Content-Type": "text/plain; charset=utf-8" } },
    );
  }
});
