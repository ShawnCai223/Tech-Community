$(function() {
    var $response = $("#esAdminResponse");
    var $recreateBtn = $("#recreateIndexBtn");
    var $syncBtn = $("#syncIndexBtn");

    if (!$response.length) {
        return;
    }

    function renderResponse(title, body, isError) {
        $response.toggleClass("is-error", !!isError);
        $response.find(".admin-tools-response-title").text(title);
        $response.find(".admin-tools-response-body").text(body);
    }

    function setBusy(isBusy) {
        $recreateBtn.prop("disabled", isBusy);
        $syncBtn.prop("disabled", isBusy);
    }

    function extractMessage(payload) {
        if (!payload) {
            return "Empty response.";
        }

        var parts = [];
        if (payload.msg) {
            parts.push(payload.msg);
        }
        if (payload.analyzer) {
            parts.push("analyzer=" + payload.analyzer);
        }
        if (payload.indexName) {
            parts.push("index=" + payload.indexName);
        }
        if (payload.synced !== undefined) {
            parts.push("synced=" + payload.synced);
        }
        if (payload.total !== undefined) {
            parts.push("total=" + payload.total);
        }
        return parts.join(" | ") || JSON.stringify(payload);
    }

    function postAction(url, loadingText) {
        setBusy(true);
        renderResponse("Running...", loadingText, false);

        $.ajax({
            url: url,
            type: "POST",
            success: function(data) {
                var ok = data && data.code === 0;
                renderResponse(ok ? "Completed" : "Request Failed", extractMessage(data), !ok);
            },
            error: function(xhr) {
                var message = "Request failed";
                if (xhr.responseJSON && xhr.responseJSON.msg) {
                    message = xhr.responseJSON.msg;
                } else if (xhr.responseText) {
                    message = xhr.responseText;
                }
                renderResponse("Request Failed", message, true);
            },
            complete: function() {
                setBusy(false);
            }
        });
    }

    $recreateBtn.on("click", function() {
        var analyzer = $("#esAnalyzer").val();
        postAction(CONTEXT_PATH + "/admin/elasticsearch/index?analyzer=" + encodeURIComponent(analyzer),
            "Recreating the discuss post index...");
    });

    $syncBtn.on("click", function() {
        postAction(CONTEXT_PATH + "/admin/elasticsearch/sync",
            "Syncing discuss posts into Elasticsearch...");
    });
});
