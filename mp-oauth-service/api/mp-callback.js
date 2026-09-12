// Handles the OAuth redirect from Mercado Pago after a user approves linking
// their account to the FactuAR app. Exchanges the authorization code for an
// access token (using the Client Secret, kept server-side only), fetches the
// account's display name, then bounces the browser to a custom URI scheme so
// the Android app picks it up directly — no polling, no database.

const MP_TOKEN_URL = "https://api.mercadopago.com/oauth/token";
const MP_ME_URL = "https://api.mercadopago.com/users/me";
const APP_SCHEME = "factuar://mp-connected";

function htmlPage({ title, message, deepLink }) {
  const redirectScript = deepLink
    ? `<script>window.location.replace(${JSON.stringify(deepLink)});</script>`
    : "";
  const linkButton = deepLink
    ? `<a href="${deepLink}" style="display:inline-block;margin-top:16px;padding:12px 20px;background:#009ee3;color:#fff;border-radius:8px;text-decoration:none;font-weight:bold;">Volver a la app</a>`
    : "";
  return `<!doctype html>
<html lang="es"><head><meta charset="utf-8"><title>${title}</title>
<meta name="viewport" content="width=device-width, initial-scale=1">
</head>
<body style="font-family:sans-serif;text-align:center;padding:40px 20px;background:#111;color:#eee;">
  <h2>${title}</h2>
  <p>${message}</p>
  ${linkButton}
  ${redirectScript}
</body></html>`;
}

export default async function handler(req, res) {
  const { code, state, error: mpError } = req.query;

  if (mpError) {
    res.status(200).send(
      htmlPage({
        title: "Autorización cancelada",
        message: "No se completó la vinculación con Mercado Pago. Podés volver a intentarlo desde la app.",
      })
    );
    return;
  }

  if (!code) {
    res.status(400).send(
      htmlPage({
        title: "Falta el código de autorización",
        message: "Mercado Pago no envió un código válido. Volvé a la app e intentá de nuevo.",
      })
    );
    return;
  }

  try {
    const tokenResponse = await fetch(MP_TOKEN_URL, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        client_id: process.env.MP_CLIENT_ID,
        client_secret: process.env.MP_CLIENT_SECRET,
        grant_type: "authorization_code",
        code,
        redirect_uri: process.env.MP_REDIRECT_URI,
      }),
    });

    const tokenData = await tokenResponse.json();
    if (!tokenResponse.ok || !tokenData.access_token) {
      res.status(200).send(
        htmlPage({
          title: "No se pudo vincular la cuenta",
          message: `Mercado Pago rechazó la autorización: ${tokenData.message || tokenData.error || "error desconocido"}.`,
        })
      );
      return;
    }

    const meResponse = await fetch(MP_ME_URL, {
      headers: { Authorization: `Bearer ${tokenData.access_token}` },
    });
    const me = meResponse.ok ? await meResponse.json() : {};

    const name = [me.first_name, me.last_name].filter(Boolean).join(" ") || me.nickname || "";

    const params = new URLSearchParams({
      access_token: tokenData.access_token,
      user_id: String(tokenData.user_id || me.id || ""),
      name,
      state: state || "",
    });
    const deepLink = `${APP_SCHEME}?${params.toString()}`;

    res.status(200).send(
      htmlPage({
        title: "¡Cuenta de Mercado Pago vinculada!",
        message: "Ya podés volver a la app FactuAR.",
        deepLink,
      })
    );
  } catch (e) {
    res.status(200).send(
      htmlPage({
        title: "Error inesperado",
        message: `Ocurrió un problema vinculando la cuenta: ${e.message}. Volvé a intentarlo desde la app.`,
      })
    );
  }
}
